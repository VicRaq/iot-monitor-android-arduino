package com.example.petmonitoriot;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

// LIBRERÍAS CRÍTICAS PARA CORREGIR ERRORES
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.util.Set;

public class DispositivosVinculadosActivity extends AppCompatActivity {

    // Componentes de la UI
    private ListView listViewDevices;

    // Lógica Bluetooth
    private BluetoothAdapter bluetoothAdapter;
    private ArrayAdapter<String> adapter;

    // Constante para pasar la dirección MAC a la siguiente pantalla
    public static final String EXTRA_DEVICE_ADDRESS = "device_address";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dispositivos_vinculados);

        listViewDevices = findViewById(R.id.listaDispositivos);

        // 1. Inicializar el adaptador Bluetooth
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "El dispositivo no soporta Bluetooth", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // 2. Verificar Permisos y Listar
        verificarPermisosYListar();
    }

    private void verificarPermisosYListar() {
        // En Android 12+ (API 31+), necesitamos pedir permiso BLUETOOTH_CONNECT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                // Pedimos el permiso si no lo tenemos (código 101)
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, 101);
                return;
            }
        }
        // Si ya tenemos permiso o es Android antiguo, listamos directo
        listarDispositivos();
    }

    private void listarDispositivos() {
        // Doble verificación de seguridad requerida por Android Studio
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return;
        }

        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);

        if (pairedDevices != null && pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                // Formato: Nombre + Salto + MAC
                adapter.add(device.getName() + "\n" + device.getAddress());
            }
        } else {
            adapter.add("No hay dispositivos vinculados");
        }

        listViewDevices.setAdapter(adapter);

        // 3. Al hacer click, enviamos la MAC a MenuActivity
        listViewDevices.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String info = ((TextView) view).getText().toString();

                if (info.equals("No hay dispositivos vinculados")) return;

                // Extraemos los últimos 17 caracteres (la dirección MAC XX:XX:XX:XX:XX:XX)
                String address = info.substring(info.length() - 17);

                // Iniciamos el Dashboard
                Intent intent = new Intent(DispositivosVinculadosActivity.this, MenuActivity.class);
                intent.putExtra(EXTRA_DEVICE_ADDRESS, address);
                startActivity(intent);
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 101) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                listarDispositivos();
            } else {
                Toast.makeText(this, "Se requiere permiso Bluetooth para continuar", Toast.LENGTH_SHORT).show();
            }
        }
    }
}