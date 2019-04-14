package org.example.parejas;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.games.Games;
import com.google.android.gms.games.GamesCallbackStatusCodes;
import com.google.android.gms.games.LeaderboardsClient;
import com.google.android.gms.games.RealTimeMultiplayerClient;
import com.google.android.gms.games.SnapshotsClient;
import com.google.android.gms.games.TurnBasedMultiplayerClient;
import com.google.android.gms.games.multiplayer.Multiplayer;
import com.google.android.gms.games.multiplayer.Participant;
import com.google.android.gms.games.multiplayer.realtime.OnRealTimeMessageReceivedListener;
import com.google.android.gms.games.multiplayer.realtime.RealTimeMessage;
import com.google.android.gms.games.multiplayer.realtime.Room;
import com.google.android.gms.games.multiplayer.realtime.RoomConfig;
import com.google.android.gms.games.multiplayer.realtime.RoomStatusUpdateCallback;
import com.google.android.gms.games.multiplayer.realtime.RoomUpdateCallback;
import com.google.android.gms.games.multiplayer.turnbased.TurnBasedMatch;
import com.google.android.gms.games.snapshot.Snapshot;
import com.google.android.gms.games.snapshot.SnapshotMetadata;
import com.google.android.gms.games.snapshot.SnapshotMetadataChange;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import static org.example.parejas.Utils.mPlayerId;

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
    String PartidaGuardadaNombre;
    private byte[] datosPartidaGuardada;
    String mRoomId = null;
    ArrayList<Participant> mParticipants = null;
    String mMyId = null;
    final static int RC_WAITING_ROOM = 10002;
    int jugadorLocal = 1;
    private RealTimeMultiplayerClient mRealTimeMultiplayerClient = null;
    RoomConfig mRoomConfig;
    final static int RC_LOOK_AT_MATCHES = 10001;
    private AlertDialog mDialogoAlerta;
    public TurnBasedMatch mMatch;
    private Turno mTurnData;
    private int turnoPartidaPorTurnos;
    private TurnBasedMultiplayerClient mTurnBasedMultiplayerClient = null;
    LeaderboardsClient mLeaderboardsClient;

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
            case "REAL":
                iniciarPartidaEnTiempoReal();
                break;
            case "TURNO":
                iniciarPartidaPorTurnos();
                break;
        }
        mLeaderboardsClient = Games.getLeaderboardsClient(this, GoogleSignIn.getLastSignedInAccount(this));
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
                    /*
                    if (Partida.tipoPartida == "REAL") {
                        int puntos;
                        if (jugadorLocal == 1) {
                            puntos = Partida.puntosJ1;
                        } else {
                            puntos = Partida.puntosJ2;
                        }
                    }
                    */
                    if (Partida.tipoPartida.equals("REAL")) {
                        int puntos;
                        if (jugadorLocal == 1) {
                            puntos = Partida.puntosJ1;
                        } else {
                            puntos = Partida.puntosJ2;
                        }
                        mLeaderboardsClient.submitScore(getString(R.string.marcador_tiempoReal_id), puntos);
                    }
                    ((TextView) findViewById(R.id.jugador)).setText("GANADOR JUGADOR " + (Partida.turno) + "");
                    if (Partida.tipoPartida.equals("TURNO")) {
                        mTurnData.puntosJ1 = Partida.puntosJ1;
                        mTurnData.puntosJ2 = Partida.puntosJ2;
                        mTurnData.turnoJugador = Partida.turno;
                        mTurnData.casillas = Partida.casillas;
                        mTurnBasedMultiplayerClient.finishMatch(mMatch.getMatchId()).addOnSuccessListener(new OnSuccessListener<TurnBasedMatch>() {
                            @Override
                            public void onSuccess(TurnBasedMatch turnBasedMatch) {
                                Toast.makeText(getApplicationContext(), "Fin de la partida.", Toast.LENGTH_LONG).show();
                                mTurnData = null;
                            }
                        }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Toast.makeText(getApplicationContext(), "Hay un problema finalizando la partida", Toast.LENGTH_SHORT);
                            }
                        });
                    }
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
                if (Partida.tipoPartida.equals("TURNO")) {
                    mTurnData.puntosJ1 = Partida.puntosJ1;
                    mTurnData.puntosJ2 = Partida.puntosJ2;
                    mTurnData.turnoJugador = Partida.turno;
                    mTurnData.casillas = Partida.casillas;
                    String nextParticipantId = dameIdSiguienteJugador();
                    mTurnBasedMultiplayerClient.takeTurn(mMatch.getMatchId(), mTurnData.persist(), nextParticipantId).addOnCompleteListener(new OnCompleteListener<TurnBasedMatch>() {
                        @Override
                        public void onComplete(@NonNull Task<TurnBasedMatch> task) {
                            if (task.isSuccessful()) {
                                TurnBasedMatch match = task.getResult();
                            }
                        }
                    });
                    Toast.makeText(getApplicationContext(), "Fin de tu turno.", Toast.LENGTH_LONG).show();
                    mTurnData = null;
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
                if (Partida.tipoPartida == "REAL") {
                    if (Partida.turno != jugadorLocal) {
                        Toast.makeText(getApplicationContext(), "No es tu turno.", Toast.LENGTH_LONG).show();
                        return;
                    }
                }
                if (primeraCasilla != null && segundaCasilla != null) {
                    return;
                }
                int id = v.getId();
                int x = id / 100;
                int y = id % 100;
                descubrirCasilla(x, y);
                if (Partida.tipoPartida == "REAL") {
                    byte[] mensaje;
                    mensaje = new byte[3];
                    mensaje[0] = (byte) 'C';
                    mensaje[1] = (byte) x;
                    mensaje[2] = (byte) y;
                    for (Participant p : mParticipants) {
                        if (!p.getParticipantId().equals(mMyId)) {
                            mRealTimeMultiplayerClient.sendReliableMessage(mensaje, mRoomId, p.getParticipantId(), new RealTimeMultiplayerClient.ReliableMessageSentCallback() {
                                @Override
                                public void onRealTimeMessageSent(int statusCode, int tokenId, String recipientParticipantId) {
                                }
                            }).addOnSuccessListener(new OnSuccessListener<Integer>() {
                                @Override
                                public void onSuccess(Integer tokenId) {
                                }
                            });
                        }
                    }
                }
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


    @Override
    public void onActivityResult(int requestCode, int responseCode, Intent intent) {
        super.onActivityResult(requestCode, responseCode, intent);
        switch (requestCode) {
            case RC_SAVED_GAMES:
                if (intent != null) {
                    if (intent.hasExtra(SnapshotsClient.EXTRA_SNAPSHOT_METADATA)) {
                        SnapshotMetadata snapshotMetadata = (SnapshotMetadata) intent.getParcelableExtra(SnapshotsClient.EXTRA_SNAPSHOT_METADATA);
                        PartidaGuardadaNombre = snapshotMetadata.getUniqueName();
                        cargarSnapshotPartidaGuardada();
                    } else {
                        if (intent.hasExtra(SnapshotsClient.EXTRA_SNAPSHOT_NEW)) {
                            nuevoSnapshotPartidaGuadada();
                        }
                    }
                } else {
                    finish();
                }
                break;
            case RC_WAITING_ROOM:
                if (responseCode == Activity.RESULT_OK) {
                    numeroJugadorLocal();
                    enviarTableroOponentes();
                    mostrarTablero();
                } else {
                    finish();
                }
                break;
            case RC_LOOK_AT_MATCHES:
                if (responseCode != Activity.RESULT_OK) {
                    return;
                }
                TurnBasedMatch match = intent.getParcelableExtra(Multiplayer.EXTRA_TURN_BASED_MATCH);
                if (match != null) {
                    gestionarPartidaTurno(match);
                }
                break;
        }
        super.onActivityResult(requestCode, responseCode, intent);
    }

    void codificaPartidaGuardada() {
        datosPartidaGuardada = new byte[Partida.FILAS * Partida.COLUMNAS];
        int k = 0;
        for (int i = 0; i < Partida.FILAS; i++) {
            for (int j = 0; j < Partida.COLUMNAS; j++) {
                datosPartidaGuardada[k] = (byte) Partida.casillas[i][j];
                k++;
            }
        }
    }

    void decodificaPartidaGuardada() {
        int i = 0;
        int j = 0;
        for (int k = 0; k < Partida.FILAS * Partida.COLUMNAS; k++) {
            Partida.casillas[i][j] = (int) datosPartidaGuardada[k];
            if (j < Partida.COLUMNAS - 1) {
                j++;
            } else {
                j = 0;
                if (i < Partida.FILAS - 1) {
                    i++;
                } else {
                    i = 0;
                }
            }
        }
    }

    void nuevoSnapshotPartidaGuadada() {
        String unique = new BigInteger(5, new Random()).toString(13);
        PartidaGuardadaNombre = "Parejas" + unique.toString();
        final SnapshotsClient snapshotsClient = Games.getSnapshotsClient(this, GoogleSignIn.getLastSignedInAccount(this));
        snapshotsClient.open(PartidaGuardadaNombre, true).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(getApplicationContext(), "Error al crear partida.", Toast.LENGTH_SHORT);
            }
        }).continueWith(new Continuation<SnapshotsClient.DataOrConflict<Snapshot>, byte[]>() {
            @Override
            public byte[] then(@NonNull Task<SnapshotsClient.DataOrConflict<Snapshot>> task) throws Exception {
                Snapshot snapshot = task.getResult().getData();
                codificaPartidaGuardada();
                snapshot.getSnapshotContents().writeBytes(datosPartidaGuardada);
                Date d = new Date();
                SnapshotMetadataChange metadataChange = new SnapshotMetadataChange.Builder().fromMetadata(snapshot.getMetadata()).setDescription("Parejas " + DateFormat.format("dd/MM/yyyy HH:mm:ss", d.getTime()).toString()).build();
                snapshotsClient.commitAndClose(snapshot, metadataChange);
                return null;
            }
        }).addOnCompleteListener(new OnCompleteListener<byte[]>() {
            @Override
            public void onComplete(@NonNull Task<byte[]> task) {
                mostrarTablero();
            }
        });
    }

    @Override
    public void onBackPressed() {
        if (Partida.tipoPartida.equals("GUARDADA")) {
            guardarPartidaGuardada();
        }
        Juego.this.finish();
    }

    public void guardarPartidaGuardada() {
        codificaPartidaGuardada();
        final SnapshotsClient snapshotsClient = Games.getSnapshotsClient(this, GoogleSignIn.getLastSignedInAccount(this));
        snapshotsClient.open(PartidaGuardadaNombre, false).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(getApplicationContext(), "Error al guardar la partida", Toast.LENGTH_SHORT);
            }
        }).continueWith(new Continuation<SnapshotsClient.DataOrConflict<Snapshot>, byte[]>() {
            @Override
            public byte[] then(@NonNull Task<SnapshotsClient.DataOrConflict<Snapshot>> task) throws Exception {
                Snapshot snapshot = task.getResult().getData();
                snapshot.getSnapshotContents().writeBytes(datosPartidaGuardada);
                SnapshotMetadataChange metadataChange = new SnapshotMetadataChange.Builder().fromMetadata(snapshot.getMetadata()).build();
                snapshotsClient.commitAndClose(snapshot, metadataChange);
                return null;
            }
        }).addOnCompleteListener(new OnCompleteListener<byte[]>() {
            @Override
            public void onComplete(@NonNull Task<byte[]> task) {
            }
        });
    }

    void cargarSnapshotPartidaGuardada() {
        final SnapshotsClient snapshotsClient = Games.getSnapshotsClient(this, GoogleSignIn.getLastSignedInAccount(this));
        snapshotsClient.open(PartidaGuardadaNombre, false).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(getApplicationContext(), "Error al cargar partida.", Toast.LENGTH_SHORT);
            }
        }).continueWith(new Continuation<SnapshotsClient.DataOrConflict<Snapshot>, byte[]>() {
            @Override
            public byte[] then(@NonNull Task<SnapshotsClient.DataOrConflict<Snapshot>> task) throws Exception {
                Snapshot snapshot = task.getResult().getData();
                datosPartidaGuardada = new byte[0];
                datosPartidaGuardada = snapshot.getSnapshotContents().readFully();
                return null;
            }
        }).addOnCompleteListener(new OnCompleteListener<byte[]>() {
            @Override
            public void onComplete(@NonNull Task<byte[]> task) {
                decodificaPartidaGuardada();
                mostrarTablero();
            }
        });
    }

    private void iniciarPartidaEnTiempoReal() {
        mRealTimeMultiplayerClient = Games.getRealTimeMultiplayerClient(this, GoogleSignIn.getLastSignedInAccount(this));
        final int NUMERO_MINIMO_OPONENTES = 1, NUMERO_MAXIMO_OPONENTES = 1;
        Bundle criterioPartidaRapida = RoomConfig.createAutoMatchCriteria(NUMERO_MINIMO_OPONENTES, NUMERO_MAXIMO_OPONENTES, 0);
        mRoomConfig = RoomConfig.builder(mRoomUpdateCallback).setOnMessageReceivedListener(mOnRealTimeMessageReceivedListener)
                .setRoomStatusUpdateCallback(mRoomStatusUpdateCallback).setAutoMatchCriteria(criterioPartidaRapida).build();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        mRealTimeMultiplayerClient.create(mRoomConfig);
    }

    private RoomUpdateCallback mRoomUpdateCallback = new RoomUpdateCallback() {
        @Override
        public void onRoomCreated(int statusCode, Room room) {
            if (statusCode != GamesCallbackStatusCodes.OK) {
                showGameError();
                return;
            }
            mRoomId = room.getRoomId();
            showWaitingRoom(room);
        }

        @Override
        public void onRoomConnected(int statusCode, Room room) {
            if (statusCode != GamesCallbackStatusCodes.OK) {
                showGameError();
                return;
            }
            updateRoom(room);
        }

        @Override
        public void onJoinedRoom(int statusCode, Room room) {
            if (statusCode != GamesCallbackStatusCodes.OK) {
                showGameError();
                return;
            }
            showWaitingRoom(room);
        }

        @Override
        public void onLeftRoom(int statusCode, @NonNull String roomId) {
        }
    };

    void updateRoom(Room room) {
        if (room != null) {
            mParticipants = room.getParticipants();
        }
    }

    void showGameError() {
        new android.app.AlertDialog.Builder(this).setMessage("Oops! Hay un error en el juego.").setNeutralButton(android.R.string.ok, null).create();
    }

    void showWaitingRoom(Room room) {
        final int MIN_PLAYERS = Integer.MAX_VALUE;
        mRealTimeMultiplayerClient.getWaitingRoomIntent(room, MIN_PLAYERS).addOnSuccessListener(new OnSuccessListener<Intent>() {
            @Override
            public void onSuccess(Intent intent) {
                startActivityForResult(intent, RC_WAITING_ROOM);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(getApplicationContext(), "Hay un problema con la partida.", Toast.LENGTH_SHORT);
            }
        });
    }

    private void numeroJugadorLocal() {
        jugadorLocal = 1;
        for (Participant p : mParticipants) {
            if (p.getParticipantId().equals(mMyId)) continue;
            if (p.getStatus() != Participant.STATUS_JOINED) continue;
            if (p.getParticipantId().compareTo(mMyId) < 0) jugadorLocal = 2;
        }
    }

    public void enviarTableroOponentes() {
        if (jugadorLocal == 1) {
            for (int fila = 0; fila < Partida.FILAS; fila++) {
                for (int columna = 0; columna < Partida.COLUMNAS; columna++) {
                    byte[] mensaje;
                    mensaje = new byte[4];
                    mensaje[0] = (byte) 'A';
                    mensaje[1] = (byte) fila;
                    mensaje[2] = (byte) columna;
                    mensaje[3] = (byte) Partida.casillas[fila][columna];
                    for (Participant p : mParticipants) {
                        if (!p.getParticipantId().equals(mMyId)) {
                            mRealTimeMultiplayerClient.sendReliableMessage(mensaje, mRoomId, p.getParticipantId(), new RealTimeMultiplayerClient.ReliableMessageSentCallback() {
                                @Override
                                public void onRealTimeMessageSent(int statusCode, int tokenId, String recipientParticipantId) {
                                }
                            }).addOnSuccessListener(new OnSuccessListener<Integer>() {
                                @Override
                                public void onSuccess(Integer tokenId) {
                                }
                            });
                        }
                    }
                }
            }
        }
    }

    void actualizaRoom(Room room) {
        if (room != null) {
            mParticipants = room.getParticipants();
        }
    }

    void mostrarErrorJuego() {
        finish();
    }

    void mostrarEsperandoARoom(Room room) {
        final int MIN_PLAYERS = Integer.MAX_VALUE;
    }

    private RoomStatusUpdateCallback mRoomStatusUpdateCallback = new RoomStatusUpdateCallback() {
        @Override
        public void onConnectedToRoom(Room room) {
            mParticipants = room.getParticipants();
            mMyId = room.getParticipantId(mPlayerId);
            if (mRoomId == null) {
                mRoomId = room.getRoomId();
            }
        }

        @Override
        public void onDisconnectedFromRoom(Room room) {
            mRoomId = null;
            mRoomConfig = null;
        }

        @Override
        public void onPeerDeclined(Room room, @NonNull List<String> arg1) {
            updateRoom(room);
        }

        @Override
        public void onPeerInvitedToRoom(Room room, @NonNull List<String> arg1) {
            updateRoom(room);
        }

        @Override
        public void onP2PDisconnected(@NonNull String participant) {
        }

        @Override
        public void onP2PConnected(@NonNull String participant) {
        }

        @Override
        public void onPeerJoined(Room room, @NonNull List<String> arg1) {
            updateRoom(room);
        }

        @Override
        public void onPeerLeft(Room room, @NonNull List<String> peersWhoLeft) {
            updateRoom(room);
        }

        @Override
        public void onRoomAutoMatching(Room room) {
            updateRoom(room);
        }

        @Override
        public void onRoomConnecting(Room room) {
            updateRoom(room);
        }

        @Override
        public void onPeersConnected(Room room, @NonNull List<String> peers) {
            updateRoom(room);
        }

        @Override
        public void onPeersDisconnected(Room room, @NonNull List<String> peers) {
            updateRoom(room);
        }
    };

    OnRealTimeMessageReceivedListener mOnRealTimeMessageReceivedListener = new OnRealTimeMessageReceivedListener() {
        @Override
        public void onRealTimeMessageReceived(@NonNull RealTimeMessage realTimeMessage) {
            byte[] buf = realTimeMessage.getMessageData();
            String sender = realTimeMessage.getSenderParticipantId();
            if (buf[0] == 'A') {
                int x = buf[1];
                int y = buf[2];
                int valor = buf[3];
                Partida.casillas[x][y] = valor;
            }
            if (buf[0] == 'C') {
                int x = buf[1];
                int y = buf[2];
                descubrirCasilla(x, y);
            }
        }
    };

    public void iniciarPartidaPorTurnos() {
        mTurnBasedMultiplayerClient = Games.getTurnBasedMultiplayerClient(this, GoogleSignIn.getLastSignedInAccount(this));
        mTurnBasedMultiplayerClient.getInboxIntent().addOnSuccessListener(new OnSuccessListener<Intent>() {
            @Override
            public void onSuccess(Intent intent) {
                startActivityForResult(intent, RC_LOOK_AT_MATCHES);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(getApplicationContext(), "Error al iniciar partida por turnos", Toast.LENGTH_SHORT);
            }
        });
    }

    public void gestionarPartidaTurno(TurnBasedMatch match) {
        mMatch = match;
        int status = match.getStatus();
        int turnStatus = match.getTurnStatus();
        switch (status) {
            case TurnBasedMatch.MATCH_STATUS_CANCELED:
                mostrarAdvertencia("Cancelada!", "Este partida ha sido cancelada!");
                return;
            case TurnBasedMatch.MATCH_STATUS_EXPIRED:
                mostrarAdvertencia("Expirada!", "Esta partida ha expirado!");
                return;
            case TurnBasedMatch.MATCH_STATUS_AUTO_MATCHING:
                mostrarAdvertencia("Esperando a jugadores aleatorios...", "Todavía estamos esperando a jugadores aleatorios.");
                return;
            case TurnBasedMatch.MATCH_STATUS_COMPLETE:
                if (turnStatus == TurnBasedMatch.MATCH_TURN_STATUS_COMPLETE) {
                    mostrarAdvertencia("Completada!", "Esta partida ha finalizado! No hay nada que hacer");
                    break;
                }
        }
        switch (turnStatus) {
            case TurnBasedMatch.MATCH_TURN_STATUS_MY_TURN:
                mTurnData = Turno.unpersist(mMatch.getData());
                if (match.getData() == null) {
                    inicializarPartidaPorTurnos(mMatch);
                }
                mostrarPartidaPorTurnos(mMatch);
                return;
            case TurnBasedMatch.MATCH_TURN_STATUS_THEIR_TURN:
                mostrarAdvertencia("Turno...", "No es tu turno.");
                break;
            case TurnBasedMatch.MATCH_TURN_STATUS_INVITED:
                mostrarAdvertencia("Esperando!", "Esperando a que respondan a las invitaciones!");
        }
        mTurnData = null;
    }

    public void mostrarAdvertencia(String title, String message) {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setTitle(title).setMessage(message);
        alertDialogBuilder.setCancelable(false).setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                finish();
            }
        });
        mDialogoAlerta = alertDialogBuilder.create();
        mDialogoAlerta.show();
    }

    public void inicializarPartidaPorTurnos(TurnBasedMatch match) {
        mTurnData = new Turno();
        mTurnData.nivel = 1;
        mTurnData.filas = 4;
        mTurnData.columnas = 4;
        mTurnData.casillas = new int[mTurnData.columnas][mTurnData.filas];
        mTurnData.puntosJ1 = 0;
        mTurnData.puntosJ2 = 0;
        mTurnData.turnoJugador = 1;
        try {
            int size = mTurnData.filas * mTurnData.columnas;
            ArrayList<Integer> list = new ArrayList<Integer>();
            for (int j = 0; j < size; j++) {
                list.add(new Integer(j));
            }
            Random r = new Random();
            for (int i = size - 1; i >= 0; i--) {
                int t = 0;
                if (i > 0) {
                    t = r.nextInt(i);
                }
                t = list.remove(t).intValue();
                mTurnData.casillas[i % mTurnData.filas][i / mTurnData.columnas] = 1 + (t % (size / 2));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        mMatch = match;
        String playerId = Utils.mPlayerId;
        String myParticipantId = mMatch.getParticipantId(playerId);
        Games.getTurnBasedMultiplayerClient(this, GoogleSignIn.getLastSignedInAccount(this)).takeTurn(match.getMatchId(), mTurnData.persist(), myParticipantId).addOnCompleteListener(new OnCompleteListener<TurnBasedMatch>() {
            @Override
            public void onComplete(@NonNull Task<TurnBasedMatch> task) {
                if (task.isSuccessful()) {
                    TurnBasedMatch match = task.getResult();
                }
            }
        });
    }

    public void mostrarPartidaPorTurnos(TurnBasedMatch match) {
        mTurnData.unpersist(match.getData());
        Partida.FILAS = mTurnData.filas;
        Partida.COLUMNAS = mTurnData.columnas;
        Partida.puntosJ1 = mTurnData.puntosJ1;
        Partida.puntosJ2 = mTurnData.puntosJ2;
        Partida.turno = mTurnData.turnoJugador;
        turnoPartidaPorTurnos = mTurnData.turnoJugador;
        Partida.casillas = new int[mTurnData.columnas][mTurnData.filas];
        Partida.casillas = mTurnData.casillas;
        mostrarTablero();
    }

    public String dameIdSiguienteJugador() {
        String playerId = Utils.mPlayerId;
        String myParticipantId = mMatch.getParticipantId(playerId);
        ArrayList<String> participantIds = mMatch.getParticipantIds();
        int desiredIndex = -1;
        for (int i = 0; i < participantIds.size(); i++) {
            if (participantIds.get(i).equals(myParticipantId)) {
                desiredIndex = i + 1;
            }
        }
        if (desiredIndex < participantIds.size()) {
            return participantIds.get(desiredIndex);
        }
        if (mMatch.getAvailableAutoMatchSlots() <= 0) {
            return participantIds.get(0);
        } else {
            return null;
        }
    }

}