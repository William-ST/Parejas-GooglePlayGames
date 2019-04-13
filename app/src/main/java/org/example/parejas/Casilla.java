package org.example.parejas;

import android.widget.Button;

public class Casilla {
    public int x;
    public int y;
    public Button boton;

    public Casilla(Button boton, int x, int y) {
        this.x = x;
        this.y = y;
        this.boton = boton;
    }
}
