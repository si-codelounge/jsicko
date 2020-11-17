/*
 * Copyright (C) 2019 Andrea Mocci and CodeLounge https://codelounge.si.usi.ch
 *
 * This file is part of jSicko - Java SImple Contract checKer.
 *
 *  jSicko is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 * jSicko is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with jSicko.  If not, see <https://www.gnu.org/licenses/>.
 *
 */

package ch.usi.si.codelounge.jsicko.tutorials.enums;

import ch.usi.si.codelounge.jsicko.Contract;

/**
 * Enum that represent a particular room type.
 */
public enum Planet implements Contract {
    MERCURY (3.303e+23, 2.4397e6),
    VENUS   (4.869e+24, 6.0518e6),
    EARTH   (5.976e+24, 6.37814e6),
    MARS    (6.421e+23, 3.3972e6),
    JUPITER (1.9e+27,   7.1492e7),
    SATURN  (5.688e+26, 6.0268e7),
    URANUS  (8.686e+25, 2.5559e7),
    NEPTUNE (1.024e+26, 2.4746e7);

    private final double mass;   // in kilograms
    private final double radius; // in meters

    Planet(double mass, double radius) {
        this.mass = mass;
        this.radius = radius;
    }

    @Pure
    public boolean approx_planet_masses() {
        switch(this) {
            case MERCURY:
                return Math.abs(this.mass() - 3.303e+23) <= Double.MIN_VALUE;
            case VENUS:
                return Math.abs(this.mass() - 4.869e+24) <= Double.MIN_VALUE;
            case EARTH:
                return Math.abs(this.mass() - 5.976e+24) <= Double.MIN_VALUE;
            case MARS:
                return Math.abs(this.mass() - 6.421e+23) <= Double.MIN_VALUE;
            case JUPITER:
                return Math.abs(this.mass() - 1.9e+27) <= Double.MIN_VALUE;
            case SATURN:
                return Math.abs(this.mass() - 5.688e+26) <= Double.MIN_VALUE;
            case URANUS:
                return Math.abs(this.mass() - 8.686e+25) <= Double.MIN_VALUE;
            case NEPTUNE:
                return Math.abs(this.mass() - 1.024e+26) <= Double.MIN_VALUE;
        }
        return false;
    }

    @Pure
    public boolean approx_planet_radiuses() {
        switch(this) {
            case MERCURY:
                return Math.abs(this.radius() - 2.4397e6) <= Double.MIN_VALUE;
            case VENUS:
                return Math.abs(this.radius() - 6.0518e6) <= Double.MIN_VALUE;
            case EARTH:
                return Math.abs(this.radius() - 6.37814e6) <= Double.MIN_VALUE;
            case MARS:
                return Math.abs(this.radius() - 3.3972e6) <= Double.MIN_VALUE;
            case JUPITER:
                return Math.abs(this.radius() - 7.1492e7) <= Double.MIN_VALUE;
            case SATURN:
                return Math.abs(this.radius() - 6.0268e7) <= Double.MIN_VALUE;
            case URANUS:
                return Math.abs(this.radius() - 2.5559e7) <= Double.MIN_VALUE;
            case NEPTUNE:
                return Math.abs(this.radius() - 2.4746e7) <= Double.MIN_VALUE;
        }
        return false;
    }

    public boolean approx_surface_gravity() {
        return Math.abs(this.surfaceGravity() - G * this.mass() / Math.pow(this.radius(), 2.0)) < 1e-10;
    }

    @Ensures("approx_planet_masses")
    public double mass() { return mass; }

    @Ensures("approx_planet_radiuses")
    public double radius() { return radius; }

    // universal gravitational constant  (m3 kg-1 s-2)
    public static final double G = 6.67300E-11;

    @Ensures("approx_surface_gravity")
    double surfaceGravity() {
        return G * mass / (radius * radius);
    }

    double surfaceWeight(double otherMass) {
        return otherMass * surfaceGravity();
    }

}