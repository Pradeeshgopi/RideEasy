const mongoose = require('mongoose');
const BusSchema = new mongoose.Schema({
    busNumber:        { type: String, required: true },
    conductorId:      { type: String, required: true },
    numberPlate:      { type: String, required: true },
    route:            { type: String, required: true },
    status:           { type: String, default: 'offline' },
    totalPassengers:  { type: Number, default: 0 },
    totalSeats:       { type: Number, default: 44 },
    occupancyPercent: { type: Number, default: 0 },
    crowdStatus:      { type: String, default: 'FREE' },
    totalRevenue:     { type: Number, default: 0 },
    ticketsIssued:    { type: Number, default: 0 },
    latitude:         { type: Number, default: 0 },
    longitude:        { type: Number, default: 0 },
    speed:            { type: Number, default: 0 },
    currentStop:      { type: String, default: '' },
    nextStop:         { type: String, default: '' },
    stopCounts:       { type: Map, of: Number, default: {} },
    lastUpdated:      { type: Date, default: Date.now }
});
module.exports = mongoose.model('Bus', BusSchema);
