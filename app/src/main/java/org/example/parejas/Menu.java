package org.example.parejas;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.games.Games;
import com.google.android.gms.games.Player;
import com.google.android.gms.games.PlayersClient;
import com.google.android.gms.games.multiplayer.Invitation;
import com.google.android.gms.games.multiplayer.Multiplayer;
import com.google.android.gms.games.multiplayer.OnInvitationReceivedListener;
import com.google.android.gms.games.multiplayer.realtime.RoomConfig;
import com.google.android.gms.games.multiplayer.turnbased.TurnBasedMatch;
import com.google.android.gms.games.multiplayer.turnbased.TurnBasedMatchConfig;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import java.util.ArrayList;
import java.util.Random;

public class Menu extends Activity implements OnInvitationReceivedListener {

    private GoogleSignInClient mGoogleSignInClient = null;
    private static final int RC_SIGN_IN = 9001;
    GoogleSignInAccount mSignedInAccount = null;
    private Button btnJugar;
    private com.google.android.gms.common.SignInButton btnConectar;
    private Button btnDesconectar;
    private Button btnPartidasGuardadas;
    private Button btnPartidaEnTiempoReal;
    String mIncomingInvitationId = null;
    final static int RC_SELECT_PLAYERS = 10000;
    private Button btnInvitar;
    private Button btnPartidaPorTurnos;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.menu);
        btnJugar = (Button) findViewById(R.id.btnJugar);

        mGoogleSignInClient = GoogleSignIn.getClient(this,
                new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_GAMES_SIGN_IN)
                        .requestScopes(Drive.SCOPE_APPFOLDER).build());

        btnConectar = (com.google.android.gms.common.SignInButton) findViewById(R.id.sign_in_button);
        btnConectar.setOnClickListener(btnConectar_Click);
        btnDesconectar = (Button) findViewById(R.id.sign_out_button);
        btnDesconectar.setOnClickListener(btnDesconectar_Click);
        btnPartidasGuardadas = (Button) findViewById(R.id.btnPartidasGuardadas);
        btnPartidaEnTiempoReal = (Button) findViewById(R.id.btnPartidaEnTiempoReal);
        btnInvitar = (Button) findViewById(R.id.btnInvitar);
        btnPartidaPorTurnos = (Button) findViewById(R.id.btnPartidaPorTurnos);
    }

    public void btnJugar_Click(View v) {
        Partida.tipoPartida = "LOCAL";
        nuevoJuego(4, 4);
        Intent intent = new Intent(this, Juego.class);
        startActivity(intent);
    }

    private void nuevoJuego(int col, int fil) {
        Partida.turno = 1;
        Partida.FILAS = fil;
        Partida.COLUMNAS = col;
        Partida.casillas = new int[Partida.COLUMNAS][Partida.FILAS];
        try {
            int size = Partida.FILAS * Partida.COLUMNAS;
            ArrayList<Integer> list = new ArrayList<Integer>();
            for (int i = 0; i < size; i++) {
                list.add(new Integer(i));
            }
            Random r = new Random();
            for (int i = size - 1; i >= 0; i--) {
                int t = 0;
                if (i > 0) {
                    t = r.nextInt(i);
                }
                t = list.remove(t).intValue();
                Partida.casillas[i % Partida.COLUMNAS][i / Partida.COLUMNAS] = 1 + (t % (size / 2));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    View.OnClickListener btnConectar_Click = new View.OnClickListener() {
        public void onClick(View v) {
            startSignInIntent();
        }
    };
    View.OnClickListener btnDesconectar_Click = new View.OnClickListener() {
        public void onClick(View v) {
            signOut();
        }
    };

    public void startSignInIntent() {
        startActivityForResult(mGoogleSignInClient.getSignInIntent(), RC_SIGN_IN);
    }

    public void signOut() {
        mGoogleSignInClient.signOut().addOnCompleteListener(this, new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    onDisconnected();
                } else {
                    Toast.makeText(getApplicationContext(), "Error al desconectar el cliente.", Toast.LENGTH_LONG);
                }
            }
        });
    }

    public void onDisconnected() {
        findViewById(R.id.sign_in_button).setVisibility(View.VISIBLE);
        findViewById(R.id.sign_out_button).setVisibility(View.GONE);
    }

    private void onConnected(GoogleSignInAccount googleSignInAccount) {
        if (mSignedInAccount != googleSignInAccount) {
            mSignedInAccount = googleSignInAccount;
            PlayersClient playersClient = Games.getPlayersClient(this, googleSignInAccount);
            playersClient.getCurrentPlayer().addOnSuccessListener(new OnSuccessListener<Player>() {
                @Override
                public void onSuccess(Player player) {
                    Utils.mPlayerId = player.getPlayerId();
                    findViewById(R.id.sign_in_button).setVisibility(View.GONE);
                    findViewById(R.id.sign_out_button).setVisibility(View.VISIBLE);
                }
            }).addOnFailureListener(new OnFailureListener() {
                public void onFailure(@NonNull Exception e) {
                    Toast.makeText(getApplicationContext(), "Hay un problema para obtener el id del jugador!", Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        signInSilently();
    }

    public void signInSilently() {
        mGoogleSignInClient.silentSignIn().addOnCompleteListener(this, new OnCompleteListener<GoogleSignInAccount>() {
            @Override
            public void onComplete(@NonNull Task<GoogleSignInAccount> task) {
                if (task.isSuccessful()) {
                    onConnected(task.getResult());
                } else {
                    onDisconnected();
                }
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int responseCode, Intent intent) {
        switch (requestCode) {
            case RC_SIGN_IN:
                Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(intent);
                try {
                    GoogleSignInAccount account = task.getResult(ApiException.class);
                    onConnected(account);
                } catch (ApiException apiException) {
                    String message = apiException.getMessage();
                    if (message == null || message.isEmpty()) {
                        message = "Error al conectar el cliente.";
                    }
                    onDisconnected();
                    new AlertDialog.Builder(this).setMessage(message).setNeutralButton(android.R.string.ok, null).show();
                }
                break;
            case RC_SELECT_PLAYERS:
                if (responseCode != Activity.RESULT_OK) {
                    return;
                }
                final ArrayList<String> invitees = intent.getStringArrayListExtra(Games.EXTRA_PLAYER_IDS);
                Bundle autoMatchCriteria = null;
                int minAutoMatchPlayers = intent.getIntExtra(Multiplayer.EXTRA_MIN_AUTOMATCH_PLAYERS, 0);
                int maxAutoMatchPlayers = intent.getIntExtra(Multiplayer.EXTRA_MAX_AUTOMATCH_PLAYERS, 0);
                if (minAutoMatchPlayers > 0) {
                    autoMatchCriteria = RoomConfig.createAutoMatchCriteria(minAutoMatchPlayers, maxAutoMatchPlayers, 0);
                } else {
                    autoMatchCriteria = null;
                }
                TurnBasedMatchConfig.Builder builder = TurnBasedMatchConfig.builder().addInvitedPlayers(invitees).setAutoMatchCriteria(autoMatchCriteria);
                builder.build();
                if (minAutoMatchPlayers > 0) {
                    builder.setAutoMatchCriteria(RoomConfig.createAutoMatchCriteria(minAutoMatchPlayers, maxAutoMatchPlayers, 0));
                }
                Games.getTurnBasedMultiplayerClient(this, GoogleSignIn.getLastSignedInAccount(this))
                        .createMatch(builder.build())
                        .addOnCompleteListener(new OnCompleteListener<TurnBasedMatch>() {
                            @Override
                            public void onComplete(@NonNull Task<TurnBasedMatch> task) {
                            }
                        });
                break;
        }
        super.onActivityResult(requestCode, responseCode, intent);
    }

    public void btnPartidasGuardadas_Click(View v) {
        Partida.tipoPartida = "GUARDADA";
        nuevoJuego(4, 4);
        Intent intent = new Intent(this, Juego.class);
        startActivity(intent);
    }

    public void btnPartidaEnTiempoReal_Click(View v) {
        Partida.tipoPartida = "REAL";
        nuevoJuego(4, 4);
        Intent intent = new Intent(this, Juego.class);
        startActivity(intent);
    }


    @Override
    public void onInvitationReceived(Invitation invitation) {
        mIncomingInvitationId = invitation.getInvitationId();
    }

    @Override
    public void onInvitationRemoved(String invitationId) {
        if (mIncomingInvitationId.equals(invitationId) && mIncomingInvitationId != null) {
            mIncomingInvitationId = null;
        }
    }

    public void btnInvitar_Click(View v) {
        final int NUMERO_MINIMO_OPONENTES = 1, NUMERO_MAXIMO_OPONENTES = 1;
        boolean allowAutoMatch = true;
        Games.getTurnBasedMultiplayerClient(this, GoogleSignIn.getLastSignedInAccount(this))
                .getSelectOpponentsIntent(NUMERO_MINIMO_OPONENTES, NUMERO_MAXIMO_OPONENTES, allowAutoMatch)
                .addOnSuccessListener(new OnSuccessListener<Intent>() {
                    @Override
                    public void onSuccess(Intent intent) {
                        startActivityForResult(intent, RC_SELECT_PLAYERS);
                    }
                });
    }

    public void btnPartidaPorTurnos_Click(View v) {
        Partida.tipoPartida = "TURNO";
        nuevoJuego(4, 4);
        Intent intent = new Intent(this, Juego.class);
        startActivity(intent);
    }


}
