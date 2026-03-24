const express = require('express');
const router = express.Router();
const Booking = require('../models/Booking');
const Bus = require('../models/Bus');

router.post('/book', async (req, res) => {
    try {
        const { busNumber, conductorId, fromStop,
                toStop, passengerCount, passengerType, fare } = req.body;
        const bookingId = 'TKT' + Date.now();
        const booking = new Booking({
            bookingId, busNumber, conductorId,
            fromStop, toStop, passengerCount, passengerType, fare
        });
        await booking.save();
        await Bus.findOneAndUpdate(
            { busNumber, conductorId },
            { $inc: { totalPassengers: passengerCount } }
        );
        const io = req.app.get('io');
        io.emit('new_booking', booking);
        res.json({ success: true, booking });
    } catch (err) {
        res.status(500).json({ success: false, message: err.message });
    }
});

router.get('/bus/:busNumber', async (req, res) => {
    try {
        const bookings = await Booking.find({
            busNumber: req.params.busNumber,
            status: 'booked'
        }).sort({ bookedAt: -1 });
        res.json({ success: true, bookings });
    } catch (err) {
        res.status(500).json({ success: false, message: err.message });
    }
});

module.exports = router;
