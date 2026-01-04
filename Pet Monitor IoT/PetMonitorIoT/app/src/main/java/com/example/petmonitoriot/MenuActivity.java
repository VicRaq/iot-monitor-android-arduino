package com.example.petmonitoriot;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MenuActivity extends AppCompatActivity {

    TextView txtTemperatura, txtNivel, txtEstado;
    Button btnAlimentar, btnLuzOn, btnLuzOff;

    String address = null;
    BluetoothAdapter bluetoothAdapter = null;
    BluetoothSocket btSocket = null;
    OutputStream outputStream;
    InputStream inputStream;
    private boolean isBtConnected = false;

    private StringBuilder recDataString = new StringBuilder();
    static final UUID myUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);

        address = getIntent().getStringExtra(DispositivosVinculadosActivity.EXTRA_DEVICE_ADDRESS);

        txtEstado = findViewById(R.id.txtEstadoConexion);
        txtTemperatura = findViewById(R.id.txtTemperatura);
        txtNivel = findViewById(R.id.txtNivelComida);
        btnAlimentar = findViewById(R.id.btnAlimentar);
        btnLuzOn = findViewById(R.id.btnLuzOn);
        btnLuzOff = findViewById(R.id.btnLuzOff);

        // Listeners simples y seguros
        btnAlimentar.setOnClickListener(v -> enviarDatos("S"));
        btnLuzOn.setOnClickListener(v -> enviarDatos("L"));
        btnLuzOff.setOnClickListener(v -> enviarDatos("O"));

        checkPermissionsAndConnect();
    }

    private void checkPermissionsAndConnect() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, 200);
                return;
            }
        }
        new Thread(this::conectarBluetooth).start();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 200 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            new Thread(this::conectarBluetooth).start();
        }
    }

    private void conectarBluetooth() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return;
        }

        try {
            if (bluetoothAdapter == null || address == null) {
                bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            }
            BluetoothDevice dispositivo = bluetoothAdapter.getRemoteDevice(address);
            bluetoothAdapter.cancelDiscovery();

            try {
                btSocket = dispositivo.createInsecureRfcommSocketToServiceRecord(myUUID);
                btSocket.connect();
            } catch (IOException e) {
                try {
                    btSocket = dispositivo.createRfcommSocketToServiceRecord(myUUID);
                    btSocket.connect();
                } catch (IOException e2) { throw e2; }
            }

            outputStream = btSocket.getOutputStream();
            inputStream = btSocket.getInputStream();
            isBtConnected = true;

            runOnUiThread(() -> {
                txtEstado.setText("Conectado: " + dispositivo.getName());
                Toast.makeText(MenuActivity.this, "Conectado", Toast.LENGTH_SHORT).show();
            });

            escucharDatos();

        } catch (IOException e) {
            isBtConnected = false;
            runOnUiThread(() -> txtEstado.setText("Error Conexión"));
        }
    }

    private void escucharDatos() {
        final byte[] buffer = new byte[1024];

        new Thread(() -> {
            while (isBtConnected) {
                try {
                    if (inputStream.available() > 0) {
                        int bytesRead = inputStream.read(buffer);
                        String readMessage = new String(buffer, 0, bytesRead);
                        recDataString.append(readMessage);

                        int endOfLineIndex = recDataString.indexOf("\n");

                        if (endOfLineIndex > 0) {
                            String dataInPrint = recDataString.substring(0, endOfLineIndex);
                            recDataString.delete(0, recDataString.length());
                            runOnUiThread(() -> procesarProtocoloUniversal(dataInPrint));
                        }
                    }
                } catch (IOException e) {
                    isBtConnected = false;
                    break;
                }
            }
        }).start();
    }

    // Parser Universal (El que lee todo bien)
    private void procesarProtocoloUniversal(String datos) {
        datos = datos.replace("\r", "").replace("\n", "").trim();

        // Debug
        txtEstado.setText("Recibiendo: " + datos);

        try {
            if (datos.contains("|")) {
                String[] partes = datos.split("\\|");
                for (String parte : partes) {
                    analizarParte(parte);
                }
            } else {
                analizarParte(datos);
            }
        } catch (Exception e) {}
    }

    private void analizarParte(String texto) {
        texto = texto.toUpperCase();
        Pattern p = Pattern.compile("(\\d+(\\.\\d+)?)");
        Matcher m = p.matcher(texto);

        String numeroEncontrado = "0";
        if (m.find()) {
            numeroEncontrado = m.group(1);
        }

        if (texto.contains("TEMP") || texto.startsWith("T:")) {
            txtTemperatura.setText(numeroEncontrado + " °C");
        }
        else if (texto.contains("NIVEL") || texto.contains("DIST") || texto.startsWith("N:")) {
            txtNivel.setText(numeroEncontrado + " cm");
        }
    }

    private void enviarDatos(String dato) {
        if (btSocket != null) {
            try { outputStream.write(dato.getBytes()); } catch (IOException e) {}
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try { if (btSocket != null) btSocket.close(); } catch (IOException e) {}
    }
}