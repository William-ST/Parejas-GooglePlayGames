package org.example.parejas;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;

public class Turno {
    public static int[][] casillas;
    public static int nivel = 0;
    public static int filas = 0;
    public static int columnas = 0;
    public static int puntosJ1 = 0;
    public static int puntosJ2 = 0;
    public static int turnoJugador = 0;

    public Turno() {
    }

    public byte[] persist() {
        JSONObject retVal = new JSONObject();
        try {
            retVal.put("nivel", nivel);
            retVal.put("filas", filas);
            retVal.put("columnas", columnas);
            retVal.put("puntosJ1", puntosJ1);
            retVal.put("puntosJ2", puntosJ2);
            retVal.put("turnoJugador", turnoJugador);
            String tablero = "";
            for (int i = 0; i < filas; i++) {
                for (int j = 0; j < columnas; j++) {
                    if (casillas[i][j] <= 9) {
                        tablero = tablero + "0" + casillas[i][j];
                    } else {
                        tablero = tablero + casillas[i][j];
                    }
                }
            }
            retVal.put("tablero", tablero);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        String st = retVal.toString();
        return st.getBytes(Charset.forName("UTF-8"));
    }

    static public Turno unpersist(byte[] byteArray) {
        if (byteArray == null) {
            return new Turno();
        }
        String st = null;
        try {
            st = new String(byteArray, "UTF-8");
        } catch (UnsupportedEncodingException e1) {
            e1.printStackTrace();
            return null;
        }
        Turno retVal = new Turno();
        try {
            JSONObject obj = new JSONObject(st);
            if (obj.has("nivel")) {
                retVal.nivel = obj.getInt("nivel");
            }
            if (obj.has("filas")) {
                retVal.filas = obj.getInt("filas");
            }
            if (obj.has("columnas")) {
                retVal.columnas = obj.getInt("columnas");
            }
            if (obj.has("puntosJ1")) {
                retVal.puntosJ1 = obj.getInt("puntosJ1");
            }
            if (obj.has("puntosJ2")) {
                retVal.puntosJ2 = obj.getInt("puntosJ2");
            }
            if (obj.has("turnoJugador")) {
                retVal.turnoJugador = obj.getInt("turnoJugador");
            }
            if (obj.has("tablero")) {
                retVal.casillas = new int[retVal.columnas][retVal.filas];
                int k = 0;
                for (int i = 0; i < retVal.filas; i++) {
                    for (int j = 0; j < retVal.columnas; j++) {
                        retVal.casillas[i][j] = Integer.parseInt(obj.getString("tablero").substring(k, k + 2));
                        k = k + 2;
                    }
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return retVal;
    }
}
