const express = require('express');
const router = express.Router();
const jwt = require('jsonwebtoken');
const bcrypt = require('bcryptjs');
const Conductor = require('../models/Conductor');

router.post('/login', async (req, res) => {
    try {
        const { conductorId, password } = req.body;
        const conductor = await Conductor.findOne({ conductorId });
        if (!conductor) return res.status(400).json({ 
            success: false, message: 'Invalid conductor ID!' });
        const isMatch = await bcrypt.compare(password, conductor.password);
        if (!isMatch) return res.status(400).json({ 
            success: false, message: 'Invalid password!' });
        const token = jwt.sign(
            { conductorId: conductor.conductorId },
            process.env.JWT_SECRET,
            { expiresIn: '24h' }
        );
        res.json({
            success: true, token,
            conductor: {
                conductorId: conductor.conductorId,
                busNumber:   conductor.busNumber,
                numberPlate: conductor.numberPlate,
                route:       conductor.route
            }
        });
    } catch (err) {
        res.status(500).json({ success: false, message: err.message });
    }
});
module.exports = router;
