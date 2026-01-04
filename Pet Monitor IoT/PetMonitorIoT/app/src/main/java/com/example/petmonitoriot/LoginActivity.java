package com.example.petmonitoriot;

// --- IMPORTACIONES ---
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {

    EditText edtUser, edtPass;
    Button btnLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Vincular componentes visuales
        edtUser = findViewById(R.id.edtUser);
        edtPass = findViewById(R.id.edtPass);
        btnLogin = findViewById(R.id.btnLogin);

        btnLogin.setOnClickListener(v -> {
            String usuario = edtUser.getText().toString();
            String password = edtPass.getText().toString();

            // Validación simple (Usuario: admin, Clave: 1234)
            if (usuario.equals("admin") && password.equals("1234")) {

                Toast.makeText(LoginActivity.this, "Bienvenido", Toast.LENGTH_SHORT).show();

                // Ir a la pantalla de selección de Bluetooth
                Intent intent = new Intent(LoginActivity.this, DispositivosVinculadosActivity.class);
                startActivity(intent);

                // Cerrar login para no volver atrás
                finish();

            } else if (usuario.isEmpty() || password.isEmpty()) {
                Toast.makeText(LoginActivity.this, "Complete los campos", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(LoginActivity.this, "Credenciales incorrectas", Toast.LENGTH_SHORT).show();
            }
        });
    }
}