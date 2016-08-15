var hashCode = function (str) {
    var hash = 0;
    if (str.length == 0) return hash;
    for (i = 0; i < str.length; i++) {
        char = str.charCodeAt(i);
        hash = ((hash << 5) - hash) + char;
        hash = hash & hash; // Convert to 32bit integer
    }
    return hash;
};

function isNumeric(n) {
    return !isNaN(parseFloat(n)) && isFinite(n);
}

function toNumeric(value, def) {
    return isNumeric(value) ? parseFloat(value) : def;
}

function toNumericAndPositive(value, def) {
    return (isNumeric(value) && value>0) ? parseFloat(value) : def;
}

function inRange(value, min, max) {
    return Math.min(Math.max(min, value), max);
}
