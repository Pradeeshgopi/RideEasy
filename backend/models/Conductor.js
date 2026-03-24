const mongoose = require('mongoose');
const ConductorSchema = new mongoose.Schema({
    conductorId: { type: String, required: true, unique: true },
    password:    { type: String, required: true },
    busNumber:   { type: String, required: true },
    numberPlate: { type: String, required: true },
    route:       { type: String, required: true },
    status:      { type: String, default: 'offline' }
});
module.exports = mongoose.model('Conductor', ConductorSchema);
