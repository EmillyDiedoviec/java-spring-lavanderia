package com.example.apilavanderia.controllers;

import com.example.apilavanderia.classes.Booking;
import com.example.apilavanderia.classes.CreateBookingDto;
import com.example.apilavanderia.classes.OutputBookingDto;
import com.example.apilavanderia.database.Database;
import com.example.apilavanderia.enums.Machine;
import org.springframework.web.bind.annotation.*;


import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/bookings")
public class BookingController {
    Database database;

    public BookingController() {
        database = new Database();
    }

    @GetMapping
    public List<OutputBookingDto> getAll() {
        return database.getBookings().stream().map(OutputBookingDto::new).toList();
    }

    @PostMapping
    public OutputBookingDto create(@RequestBody CreateBookingDto newBooking) {
        var apt = database.getApartmentByNumber(newBooking.apartment());
        var bookings = database.getBookings();

        // Verificar se usuário já possui agendamento no range de +-4 dias

        var filterApt = bookings.stream()
                .filter(b -> b.getApartment().equals(apt))
                .filter(b -> newBooking.date().minusDays(4).isAfter(b.getDate()))
                .filter(b -> newBooking.date().plusDays(4).isBefore(b.getDate()))
                .toList();


        if (filterApt.size() > 0)
            throw new RuntimeException("Usuário com agendamento no período de +-4 dias!");


        // Verificar se tem agendamento para mesma data
        var filteredList = bookings.stream()
                .filter(b -> b.getDate().equals(newBooking.date()))
                .filter(b -> b.getMachine().equals(newBooking.machine()))
                .toList();

        if (filteredList.size() > 0) {
            // verifica se maquina esta ocupada no dia
            for (Booking b : filteredList) {
                // Verificar se máquina está reservada para X hora
                if (b.getHour().equals(newBooking.hour())) {
                    throw new RuntimeException("Máquina já agendada neste horário.");
                }
            }
        }

        var booking = new Booking(newBooking, apt);
        database.addBookings(booking);

        return new OutputBookingDto(booking);
    }


    @DeleteMapping("/{id}")
    public String delete(@PathVariable int id) {
        var deletedId = database.getBookings().removeIf(b -> b.getId() == id);
        if (deletedId) {
            return "A reserva foi excluída";
        }
        return "Reserva não encontrada";

    }

}