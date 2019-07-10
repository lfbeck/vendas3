package br.edu.ifsul.vendas.activity;

import android.content.DialogInterface;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import br.edu.ifsul.vendas.R;
import br.edu.ifsul.vendas.adapter.CarrinhoAdapter;
import br.edu.ifsul.vendas.model.ItemPedido;
import br.edu.ifsul.vendas.model.Pedido;
import br.edu.ifsul.vendas.model.Produto;
import br.edu.ifsul.vendas.setup.AppSetup;

public class CarrinhoActivity extends AppCompatActivity {

    private ListView lv_carrinho;
    private TextView tvClienteCarrinho;
    private TextView tvTotalPedidoCarrinho;
    public int i;
    private Double valorTotal = new Double(0);
    private List<ItemPedido> itens;
    private Produto produto;
    private ItemPedido itemPedido;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_carrinho);

        lv_carrinho = findViewById(R.id.lv_carrinho);

        tvClienteCarrinho = findViewById(R.id.tvClienteCarrinho);
        tvTotalPedidoCarrinho = findViewById(R.id.tvTotalPedidoCarrinho);

        tvClienteCarrinho.setText(AppSetup.cliente.getNome().toString());

        lv_carrinho.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                adExcluirItem("Atenção", "Você deseja excluir esse item?", position);
                return true;
            }
        });

        lv_carrinho.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(CarrinhoActivity.this, ProdutoDetalheActivity.class);
                intent.putExtra("position", AppSetup.carrinho.get(position).getProduto().getIndex());
                atualizaEstoque(position);//
                startActivity(intent);//
                AppSetup.carrinho.remove(position);
                finish();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!AppSetup.carrinho.isEmpty()) {
            atualizaView();
        }

        //copia o carrinho para usar na att do estoque
        itens = new ArrayList<>();
        itens.addAll(AppSetup.carrinho);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_activity_carrinho, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menuitem_salvar:
                if (AppSetup.carrinho.isEmpty()) {
                    Toast.makeText(this, "Carrinho vazio.", Toast.LENGTH_SHORT).show();
                } else {
                    adSalvarPedido("Processando...", "\nTotal = " + NumberFormat.getCurrencyInstance().format(valorTotal) + ". Confirmar?");
                }

                //Toast.makeText(this, "Salvar no banco", Toast.LENGTH_SHORT).show();

                break;

            case R.id.menuitem_cancelar:
                if (AppSetup.carrinho.size() != 0) {
                    adCancelarPedido("Cancelamento de Pedido", "Você realmente deseja cancelar o pedido?");
                } else {
                    Toast.makeText(this, "O carrinho está vazio!", Toast.LENGTH_SHORT).show();
                }

                //Toast.makeText(this, "Excluir carrinho", Toast.LENGTH_SHORT).show();

                break;
        }

        return true;
    }

    private void atualizaView() {
        lv_carrinho.setAdapter(new CarrinhoAdapter(CarrinhoActivity.this, AppSetup.carrinho));

        valorTotal = new Double(0);

        for (ItemPedido itemPedido : AppSetup.carrinho) {
            valorTotal += itemPedido.getTotalItem();
        }

        tvTotalPedidoCarrinho.setText(NumberFormat.getCurrencyInstance().format(valorTotal));
    }

    private void atualizaEstoque(final int position) {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        final DatabaseReference myRef = database.getReference("produtos/").child(itens.get(position).getProduto().getKey()).child("quantidade");

        myRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                //referencia da posição do estoque
                long quantidade = (long) dataSnapshot.getValue();

                myRef.setValue(itens.get(position).getQuantidade() + quantidade);
                atualizaView();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void adSalvarPedido(String titulo, String mensagem) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setTitle(titulo);
        builder.setMessage(mensagem);

        builder.setPositiveButton(R.string.alertdialog_sim, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //referencia do banco
                FirebaseDatabase database = FirebaseDatabase.getInstance();
                DatabaseReference myRef = database.getReference("pedidos/");

                Pedido pedido = new Pedido();
                String key = myRef.push().getKey();
                pedido.setFormaDePagamento("dinheiro");
                pedido.setEstado("aberto");
                pedido.setDataCriacao(Calendar.getInstance().getTime());
                pedido.setDataModificacao(Calendar.getInstance().getTime());
                pedido.setTotalPedido(valorTotal);
                pedido.setSituacao(true);
                pedido.setItens(AppSetup.carrinho);
                pedido.setCliente(AppSetup.cliente);

                //salva no db
                myRef.child(key).setValue(pedido);

                DatabaseReference myRef2 = database.getReference("clientes");
                List<String> pedidos = new ArrayList<>();
                pedidos.addAll(AppSetup.cliente.getPedidos());
                pedidos.add(key);
                AppSetup.cliente.setPedidos(pedidos);
                myRef2.child(AppSetup.cliente.getKey()).setValue(AppSetup.cliente);

                AppSetup.carrinho.clear();
                AppSetup.cliente = null;
                AppSetup.pedidos = null;

                Toast.makeText(CarrinhoActivity.this, "Vendido com sucesso!", Toast.LENGTH_SHORT).show();

                finish();
            }
        });

        builder.setNegativeButton(R.string.alertdialog_nao, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Toast.makeText(CarrinhoActivity.this, "Venda cancelada!", Toast.LENGTH_SHORT).show();
            }
        });
        builder.show();
    }

    private void adCancelarPedido(String titulo, String mensagem) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setTitle(titulo);
        builder.setMessage(mensagem);

        builder.setPositiveButton(R.string.alertdialog_sim, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                for (int j = 0; j < itens.size(); j++) {
                    atualizaEstoque(i);
                }

                AppSetup.carrinho.clear();
                AppSetup.cliente = null;

                Toast.makeText(CarrinhoActivity.this, "Pedido cancelado!", Toast.LENGTH_SHORT).show();
                finish();
            }
        });

        builder.setNegativeButton(R.string.alertdialog_nao, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Toast.makeText(CarrinhoActivity.this, "Operação cancelada!", Toast.LENGTH_SHORT).show();
            }
        });

        builder.show();
    }

    private void adExcluirItem(String titulo, String mensagem, final int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setTitle(titulo);
        builder.setMessage(mensagem);

        builder.setPositiveButton(R.string.alertdialog_sim, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                AppSetup.carrinho.remove(position);
                Toast.makeText(CarrinhoActivity.this, "Produto removido!", Toast.LENGTH_SHORT).show();

                atualizaView();
                atualizaEstoque(position);
            }
        });

        builder.show();
    }
}
