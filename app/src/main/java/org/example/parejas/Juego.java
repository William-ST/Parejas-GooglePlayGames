package org.example.parejas;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.games.Games;
import com.google.android.gms.games.SnapshotsClient;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class Juego extends Activity {
    private Drawable imagenOculta;
    private List<Drawable> imagenes;
    private Casilla primeraCasilla;
    private Casilla segundaCasilla;
    private ButtonListener botonListener;
    private TableLayout tabla;
    private actualizaCasillas handler;
    private Context context;
    private static Object lock = new Object();
    private Button[][] botones;
    private ButtonListener btnCasilla_Click;
    private static final int RC_SAVED_GAMES = 9009;
    SnapshotsClient snapshotsClient;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        handler = new actualizaCasillas();
        cargarImagenes();
        setContentView(R.layout.juego);
        imagenOculta = getResources().getDrawable(R.drawable.icon);
        tabla = (TableLayout) findViewById(R.id.TableLayoutCasilla);
        context = tabla.getContext();
        btnCasilla_Click = new ButtonListener();
        switch (Partida.tipoPartida) {
            case "LOCAL":
                mostrarTablero();
                break;
            case "GUARDADA":
                mostrarPartidasGuardadas();
                break;
        }
    }

    class actualizaCasillas extends Handler {
        @Override
        public void handleMessage(Message msg) {
            synchronized (lock) {
                compruebaCasillas();
            }
        }

        public void compruebaCasillas() {
            if (Partida.casillas[segundaCasilla.x][segundaCasilla.y] == Partida.casillas[primeraCasilla.x][primeraCasilla.y]) {
                //ACIERTO
                Partida.casillas[segundaCasilla.x][segundaCasilla.y] = 0;
                Partida.casillas[primeraCasilla.x][primeraCasilla.y] = 0;
                botones[primeraCasilla.x][primeraCasilla.y].setVisibility(View.INVISIBLE);
                botones[segundaCasilla.x][segundaCasilla.y].setVisibility(View.INVISIBLE);
                if (Partida.turno == 1) {
                    Partida.puntosJ1 += 2;
                } else {
                    Partida.puntosJ2 += 2;
                }
                if ((Partida.puntosJ1 + Partida.puntosJ2) == (Partida.FILAS * Partida.COLUMNAS)) {
                    //FIN JUEGO
                    ((TextView) findViewById(R.id.jugador)).setText("GANADOR JUGADOR " + (Partida.turno) + "");
                }
            } else {
                //FALLO
                segundaCasilla.boton.setBackgroundDrawable(imagenOculta);
                primeraCasilla.boton.setBackgroundDrawable(imagenOculta);
                if (Partida.turno == 1) {
                    Partida.turno = 2;
                } else {
                    Partida.turno = 1;
                }
            }
            primeraCasilla = null;
            segundaCasilla = null;
        }
    }

    private void cargarImagenes() {
        imagenes = new ArrayList<Drawable>();
        imagenes.add(getResources().getDrawable(R.drawable.card1));
        imagenes.add(getResources().getDrawable(R.drawable.card2));
        imagenes.add(getResources().getDrawable(R.drawable.card3));
        imagenes.add(getResources().getDrawable(R.drawable.card4));
        imagenes.add(getResources().getDrawable(R.drawable.card5));
        imagenes.add(getResources().getDrawable(R.drawable.card6));
        imagenes.add(getResources().getDrawable(R.drawable.card7));
        imagenes.add(getResources().getDrawable(R.drawable.card8));
        imagenes.add(getResources().getDrawable(R.drawable.card9));
        imagenes.add(getResources().getDrawable(R.drawable.card10));
        imagenes.add(getResources().getDrawable(R.drawable.card11));
        imagenes.add(getResources().getDrawable(R.drawable.card12));
        imagenes.add(getResources().getDrawable(R.drawable.card13));
        imagenes.add(getResources().getDrawable(R.drawable.card14));
        imagenes.add(getResources().getDrawable(R.drawable.card15));
        imagenes.add(getResources().getDrawable(R.drawable.card16));
        imagenes.add(getResources().getDrawable(R.drawable.card17));
        imagenes.add(getResources().getDrawable(R.drawable.card18));
        imagenes.add(getResources().getDrawable(R.drawable.card19));
        imagenes.add(getResources().getDrawable(R.drawable.card20));
        imagenes.add(getResources().getDrawable(R.drawable.card21));
    }

    class ButtonListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            synchronized (lock) {
                if (primeraCasilla != null && segundaCasilla != null) {
                    return;
                }
                int id = v.getId();
                int x = id / 100;
                int y = id % 100;
                descubrirCasilla(x, y);
            }
        }
    }

    private void descubrirCasilla(int x, int y) {
        Button button = botones[x][y];
        button.setBackgroundDrawable(imagenes.get(Partida.casillas[x][y]));
        if (primeraCasilla == null) {
            primeraCasilla = new Casilla(button, x, y);
        } else {
            if (primeraCasilla.x == x && primeraCasilla.y == y) {
                return;
            }
            segundaCasilla = new Casilla(button, x, y);
            ((TextView) findViewById(R.id.marcador)).setText("JUGADOR 1= " + (Partida.puntosJ1) + " : JUGADOR 2= " + (Partida.puntosJ2));
            ((TextView) findViewById(R.id.jugador)).setText("TURNO JUGADOR " + (Partida.turno) + "");
            TimerTask tt = new TimerTask() {
                @Override
                public void run() {
                    try {
                        synchronized (lock) {
                            handler.sendEmptyMessage(0);
                        }
                    } catch (Exception e) {
                        Log.e("E1", e.getMessage());
                    }
                }
            };
            Timer t = new Timer(false);
            t.schedule(tt, 1300);
        }
    }

    private void mostrarTablero() {
        botones = new Button[Partida.COLUMNAS][Partida.FILAS];
        for (int y = 0; y < Partida.FILAS; y++) {
            tabla.addView(crearFila(y));
        }
        ((TextView) findViewById(R.id.marcador)).setText("JUGADOR 1= " + (Partida.puntosJ1) + " : JUGADOR 2= " + (Partida.puntosJ2));
        ((TextView) findViewById(R.id.jugador)).setText("TURNO JUGADOR " + (Partida.turno) + "");
    }

    private TableRow crearFila(int y) {
        TableRow row = new TableRow(context);
        row.setHorizontalGravity(Gravity.CENTER);
        for (int x = 0; x < Partida.COLUMNAS; x++) {
            row.addView(crearCasilla(x, y));
            if (Partida.casillas[x][y] == 0) {
                botones[x][y].setVisibility(View.INVISIBLE);
            }
        }
        return row;
    }

    private View crearCasilla(int x, int y) {
        Button button = new Button(context);
        button.setBackgroundDrawable(imagenOculta);
        button.setId(100 * x + y);
        button.setOnClickListener(btnCasilla_Click);
        botones[x][y] = button;
        return button;
    }

    private void mostrarPartidasGuardadas() {
        SnapshotsClient snapshotsClient = Games.getSnapshotsClient(this, GoogleSignIn.getLastSignedInAccount(this));
        int maxNumberOfSavedGamesToShow = 5;
        Task<Intent> intentTask = snapshotsClient.getSelectSnapshotIntent("Partidas guardadas", true, true, maxNumberOfSavedGamesToShow);
        intentTask.addOnSuccessListener(new OnSuccessListener<Intent>() {
            @Override
            public void onSuccess(Intent intent) {
                startActivityForResult(intent, RC_SAVED_GAMES);
            }
        });
    }
}