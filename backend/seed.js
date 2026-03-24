const mongoose = require('mongoose');
const bcrypt = require('bcryptjs');
require('dotenv').config();
const Conductor = require('./models/Conductor');

const conductors = [
    { conductorId: 'CON01', password: 'pass123', busNumber: '99A', numberPlate: 'TN 01 BE 2134', route: 'Tambaram to Medavakkam' },
    { conductorId: 'CON02', password: 'pass123', busNumber: '99A', numberPlate: 'TN 02 CX 5678', route: 'Tambaram to Medavakkam' },
    { conductorId: 'CON03', password: 'pass123', busNumber: '119', numberPlate: 'TN 07 AK 9012', route: 'Guindy to Sholinganallur' },
    { conductorId: 'CON04', password: 'pass123', busNumber: '119', numberPlate: 'TN 04 DR 3456', route: 'Guindy to Sholinganallur' }
];

async function seed() {
    try {
        await mongoose.connect(process.env.MONGO_URI);
        console.log('MongoDB connected!');
        await Conductor.deleteMany({});
        for (const c of conductors) {
            const hashed = await bcrypt.hash(c.password, 10);
            await Conductor.create({ ...c, password: hashed });
            console.log('Created:', c.conductorId);
        }
        console.log('Database seeded successfully!');
        process.exit(0);
    } catch (err) {
        console.error('Error:', err.message);
        process.exit(1);
    }
}
seed();
