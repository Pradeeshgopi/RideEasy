const express = require('express');
const router = express.Router();
const Bus = require('../models/Bus');
const authMiddleware = require('../middleware/auth');

router.get('/', async (req, res) => {
    try {
        const buses = await Bus.find({ status: 'online' });
        res.json({ success: true, buses });
    } catch (err) {
        res.status(500).json({ success: false, message: err.message });
    }
});

router.get('/:busNumber/:conductorId', async (req, res) => {
    try {
        const bus = await Bus.findOne({ 
            busNumber:   req.params.busNumber,
            conductorId: req.params.conductorId
        });
        if (!bus) return res.status(404).json({ 
            success: false, message: 'Bus not found!' });
        res.json({ success: true, bus });
    } catch (err) {
        res.status(500).json({ success: false, message: err.message });
    }
});

router.post('/update', authMiddleware, async (req, res) => {
    try {
        const data = req.body;
        const bus = await Bus.findOneAndUpdate(
            { busNumber: data.busNumber, conductorId: data.conductorId },
            { ...data, status: 'online', lastUpdated: new Date() },
            { upsert: true, returnDocument: 'after' }
        );
        const io = req.app.get('io');
        io.emit('bus_data_changed', bus);
        res.json({ success: true, bus });
    } catch (err) {
        res.status(500).json({ success: false, message: err.message });
    }
});

router.post('/location', authMiddleware, async (req, res) => {
    try {
        const { conductorId, busNumber, 
                latitude, longitude, speed } = req.body;
        await Bus.findOneAndUpdate(
            { busNumber, conductorId },
            { latitude, longitude, speed, lastUpdated: new Date() }
        );
        const io = req.app.get('io');
        io.emit('bus_location_changed', { 
            busNumber, conductorId, 
            latitude, longitude, speed 
        });
        res.json({ success: true });
    } catch (err) {
        res.status(500).json({ success: false, message: err.message });
    }
});

router.post('/end-shift', authMiddleware, async (req, res) => {
    try {
        const { conductorId, busNumber } = req.body;
        await Bus.findOneAndUpdate(
            { busNumber, conductorId },
            { status: 'offline', totalPassengers: 0, 
              lastUpdated: new Date() }
        );
        const io = req.app.get('io');
        io.emit('bus_offline', { busNumber, conductorId });
        res.json({ success: true, message: 'Shift ended!' });
    } catch (err) {
        res.status(500).json({ success: false, message: err.message });
    }
});
router.post('/camera', authMiddleware, async (req, res) => {
    try {
        const { conductorId, busNumber, 
                cameraCount } = req.body;
        const bus = await Bus.findOneAndUpdate(
            { busNumber, conductorId },
            { 
                totalPassengers: cameraCount,
                lastUpdated: new Date()
            },
            { new: true }
        );
        const io = req.app.get('io');
        io.emit('bus_data_changed', bus);
        res.json({ success: true });
    } catch (err) {
        res.status(500).json({ 
            success: false, 
            message: err.message 
        });
    }
});
module.exports = router;
