const jwt = require('jsonwebtoken');
module.exports = (req, res, next) => {
    const token = req.header('Authorization')?.replace('Bearer ', '');
    if (!token) return res.status(401).json({ 
        success: false, message: 'No token!' });
    try {
        const decoded = jwt.verify(token, process.env.JWT_SECRET);
        req.conductor = decoded;
        next();
    } catch (err) {
        res.status(401).json({ success: false, message: 'Invalid token!' });
    }
};
