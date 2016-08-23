var D3Functions = {
    // color: d3.scaleOrdinal(d3.schemeCategory20),
    color: d3.scaleOrdinal(d3.schemePastel1),
    getR: function (d) {
        // var points = isNumeric(d.data.points) ? d.data.points : 0;
        // return d.data.radius * (3 + (points / 100)) + 10;
        return d.data.radius * 3 + 10;
    },
    getName: function (d) {
        return d.data.name;
    },
    getClusterName: function (d) {
        return d.data.cluster;
    },
    getKey: function (d) {
        return d.data.key;
    },
    getColor: function (d) {
        return d.data.color;
    },
    getColorByName: function (d) {
        return d3.rgb(D3Functions.color(hashCode(D3Functions.getName(d))));
    },
    getColorByNameWithLevel: function (d) {
        var level = isNumeric(d.data.level) ? d.data.level : 0;
        return D3Functions.getColorByName(d).darker(level / 20);
    },
    getFontSizeByName: function (d) {
        return (D3Functions.getR(d) * (4 / D3Functions.getName(d).length)) + "px";
    },
    getLevel: function (d) {
        if(d.data.primary) {
            return d.data.level ? d.data.level : 0;
        }
        return "";
    },
    getPoints: function (d) {
        return d.data.points ? d.data.points : 0;
    },
    isClusterLevel: function (d) {
        return d.depth === 1;
    },
    isSessionLevel: function (d) {
        return d.depth === 2;
    },
    isPrimary: function (d) {
        return d.data.nodeType === "PRIMARY";
    },
    filterByName: function (name) {
        return function (d) {
            return D3Functions.getName(d) === name;
        }
    },
    getAttrY: function (d) {
        return d.y;
    },
    getAttrX: function (d) {
        return d.x;
    },
    strengthByDistance: function (center, power, accessor) {
        return function (node) {
            var distance = Math.abs(accessor(node) - center) / center;
            return Math.pow(distance, power);
        }
    },

    inScreenWidth: function (v) {
        return inRange(toNumeric(v, window.innerWidth / 2), 0, window.innerWidth);
    },
    inScreenHeight: function (v) {
        return inRange(toNumeric(v, window.innerHeight / 2), 0, window.innerHeight);
    },

    getLinkTargetKey: function (d) {
        return D3Functions.getKey(d.target);
    }
};