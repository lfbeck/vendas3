package br.edu.ifsul.vendas.activity;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

import br.edu.ifsul.vendas.R;
import br.edu.ifsul.vendas.adapter.PedidosAdapter;
import br.edu.ifsul.vendas.barcode.BarcodeCaptureActivity;
import br.edu.ifsul.vendas.model.Cliente;
import br.edu.ifsul.vendas.model.Pedido;
import br.edu.ifsul.vendas.setup.AppSetup;

public class PedidosActivity extends AppCompatActivity {
    private ListView lv_pedidos;
    private static final String TAG = "pedidosactivity";
    private List<Pedido> pedidos = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pedidos);

        //ativa o botão home na actionbar
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        lv_pedidos = findViewById(R.id.lv_pedidos);
        lv_pedidos.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

            }
        });
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        AppSetup.pedidos.clear();
//        final List<String> pedidos = new ArrayList<>();
//        DatabaseReference myRef = database.getReference("pedidos");
//        myRef.addListenerForSingleValueEvent(new ValueEventListener() {
//            @Override
//            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
//                for(DataSnapshot ds: dataSnapshot.getChildren()) {
//                    if(ds.child(ds.getKey()).child("cliente").equals(AppSetup.cliente.getCodigoDeBarras())) {
//                        pedidos.add(ds.child(ds.getKey()).toString());
//                    }
//                }
//                AppSetup.cliente.setPedidos(pedidos);
//            }
//
//            @Override
//            public void onCancelled(@NonNull DatabaseError databaseError) {
//
//            }
//        });

        Toast.makeText(PedidosActivity.this, "appsetup: "+AppSetup.cliente.getPedidos().toString(), Toast.LENGTH_SHORT).show();

        for (String key : AppSetup.cliente.getPedidos()) {
            Log.d("pedidosactivity", AppSetup.cliente.getPedidos().toString());
            if (!key.equals(" ")) {
                DatabaseReference myRef2 = database.getReference("pedidos").child(key);
                myRef2.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        Pedido pedido = dataSnapshot.getValue(Pedido.class);
                        pedidos.add(pedido);
                        Log.d("pedidosactivity", "Dado Pedido em onDataChange PedidosActivity" + pedidos);
                        lv_pedidos.setAdapter(new PedidosAdapter(PedidosActivity.this, pedidos));
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        // Failed to read value
                        Log.w(TAG, "Failed to read value.", error.toException());
                    }
                });
            }
        }

        //carrega os dados na View


    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
        }
        return true;
    }
}