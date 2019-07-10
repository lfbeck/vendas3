package br.edu.ifsul.vendas.activity;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import java.text.NumberFormat;

import br.edu.ifsul.vendas.R;
import br.edu.ifsul.vendas.model.ItemPedido;
import br.edu.ifsul.vendas.model.Produto;
import br.edu.ifsul.vendas.setup.AppSetup;

public class ProdutoDetalheActivity extends AppCompatActivity {

    private static final String TAG = "produtoDetalheActivity";
    private TextView tvNome, tvDescricao, tvValor, tvEstoque;
    private EditText etQuantidade;
    private ImageView imvFoto;
    private Button btVender;
    private Produto produto;
    private ProgressBar pbFoto;

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_produto_detalhe);

        //ativa o botão home na actionbar
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        //mapeia os componentes da UI
        tvNome = findViewById(R.id.tvNomeProduto);
        tvDescricao = findViewById(R.id.tvDerscricaoProduto);
        tvValor = findViewById(R.id.tvValorProduto);
        tvEstoque = findViewById(R.id.tvQuantidadeProduto);
        etQuantidade = findViewById(R.id.etQuantidade);
        imvFoto = findViewById(R.id.imvFoto);
        btVender = findViewById(R.id.btComprarProduto);

        //obtém a position anexada como metadado
        final Integer position = getIntent().getExtras().getInt("position");
        produto = AppSetup.produtos.get(position);
        Log.d(TAG, ""+ produto.equals(AppSetup.produtos.get(position)));

        //bindview
        tvNome.setText(produto.getNome());
        tvDescricao.setText(produto.getDescricao());
        tvValor.setText(NumberFormat.getCurrencyInstance().format(produto.getValor()));

        if(produto.getUrl_foto() != ""){
            //carrega a imagem aqui
//            if(AppSetup.cacheProdutos.get(produto.getKey()) == null){
//                StorageReference mStorageRef = FirebaseStorage.getInstance().getReference("produtos/" + produto.getCodigoDeBarras() + ".jpeg");
//                final long ONE_MEGABYTE = 1024 * 1024;
//                mStorageRef.getBytes(ONE_MEGABYTE).addOnSuccessListener(new OnSuccessListener<byte[]>() {
//                    @Override
//                    public void onSuccess(byte[] bytes) {
//                        fotoEmBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
//                        imvFoto.setImageBitmap(fotoEmBitmap);
//                        AppSetup.cacheProdutos.put(produto.getKey(), fotoEmBitmap);
//                        pbFoto.setVisibility(ProgressBar.INVISIBLE);
//                    }
//                }).addOnFailureListener(new OnFailureListener() {
//                    @Override
//                    public void onFailure(@NonNull Exception exception) {
//                        Log.d(TAG, "Download da foto do produto falhou: " + "produtos/" + produto.getCodigoDeBarras() + ".jpeg");
//                    }
//                });
//            }else{
//                imvFoto.setImageBitmap(AppSetup.cacheProdutos.get(produto.getKey()));
//                pbFoto.setVisibility(ProgressBar.INVISIBLE);
//            }
        }

        // obtém a referência do database e do nó
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference myRef = database.getReference("produtos/" + produto.getKey() + "/quantidade");

        // Escuta o database
        myRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Integer quantidade = dataSnapshot.getValue(Integer.class);
                tvEstoque.setText(String.format("%s %s", getString(R.string.label_estoque), quantidade.toString()));
                produto.setQuantidade(quantidade);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        btVender.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(AppSetup.cliente == null){
                    startActivity(new Intent(ProdutoDetalheActivity.this, ClientesActivity.class));
                }else{
                    if(etQuantidade.getText().toString().isEmpty()){
                        Toast.makeText(ProdutoDetalheActivity.this, "Digite a quantidade.", Toast.LENGTH_SHORT).show();
                    }else{
                        Integer quantidade = Integer.valueOf(etQuantidade.getText().toString());
                        if(quantidade <= produto.getQuantidade()){
                            ItemPedido item = new ItemPedido();
                            item.setProduto(produto);
                            item.setQuantidade(quantidade);
                            item.setTotalItem(quantidade * produto.getValor());
                            item.setSituacao(true);
                            AppSetup.carrinho.add(item);
                            FirebaseDatabase database = FirebaseDatabase.getInstance();
                            DatabaseReference qtdEstoque = database.getReference("produtos/" + produto.getKey() + "/quantidade");
                            qtdEstoque.setValue(AppSetup.produtos.get(position).getQuantidade() - item.getQuantidade());
                            Toast.makeText(ProdutoDetalheActivity.this, getString(R.string.toast_adicionado_ao_carrinho), Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(ProdutoDetalheActivity.this, CarrinhoActivity.class));
                            finish();
                        }else{
                            Toast.makeText(ProdutoDetalheActivity.this, "Quantidade acima do estoque.", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            }
        });
    }
}
