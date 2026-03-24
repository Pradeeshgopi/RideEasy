const express = require('express');
const http = require('http');
const socketio = require('socket.io');
const mongoose = require('mongoose');
const cors = require('cors');
require('dotenv').config();

const app = express();
const server = http.createServer(app);
const io = socketio(server, {
    cors: { origin: '*', methods: ['GET', 'POST', 'PUT'] }
});

app.use(cors());
app.use(express.json());

app.use('/api/auth',     require('./routes/auth'));
app.use('/api/buses',    require('./routes/buses'));
app.use('/api/bookings', require('./routes/bookings'));

app.get('/', (req, res) => {
    res.json({
        status: 'success',
        message: 'RideEasy Backend Running!',
        version: '1.0.0'
    });
});

io.on('connection', (socket) => {
    console.log('Device connected:', socket.id);
    socket.on('bus_update', (data) => {
        io.emit('bus_data_changed', data);
    });
    socket.on('location_update', (data) => {
        io.emit('bus_location_changed', data);
    });
    socket.on('disconnect', () => {
        console.log('Device disconnected:', socket.id);
    });
});

app.set('io', io);

const PORT = process.env.PORT || 3000;

mongoose.connect(process.env.MONGO_URI)
.then(() => {
    console.log('MongoDB connected!');
    server.listen(PORT, '0.0.0.0', () => {
        console.log(`Server running on port ${PORT}`);
        const { networkInterfaces } = require('os');
        const nets = networkInterfaces();
        for (const name of Object.keys(nets)) {
            for (const net of nets[name]) {
                if (net.family === 'IPv4' && !net.internal) {
                    console.log(`Android connect to: http://${net.address}:${PORT}`);
                }
            }
        }
    });
})
.catch(err => console.error('MongoDB error:', err.message));
