const mongoose = require('mongoose');
const BookingSchema = new mongoose.Schema({
    bookingId:      { type: String, required: true, unique: true },
    busNumber:      { type: String, required: true },
    conductorId:    { type: String, required: true },
    fromStop:       { type: String, required: true },
    toStop:         { type: String, required: true },
    passengerCount: { type: Number, required: true },
    passengerType:  { type: String, default: 'Adult' },
    fare:           { type: Number, required: true },
    status:         { type: String, default: 'booked' },
    bookedAt:       { type: Date, default: Date.now }
});
module.exports = mongoose.model('Booking', BookingSchema);
