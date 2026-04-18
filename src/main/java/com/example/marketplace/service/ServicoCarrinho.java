package com.example.marketplace.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import java.math.RoundingMode;

import com.example.marketplace.model.CategoriaProduto;
import com.example.marketplace.model.ItemCarrinho;
import com.example.marketplace.model.Produto;
import com.example.marketplace.model.ResumoCarrinho;
import com.example.marketplace.model.SelecaoCarrinho;
import com.example.marketplace.repository.ProdutoRepository;

@Service
public class ServicoCarrinho {

    private final ProdutoRepository repositorioProdutos;

    public ServicoCarrinho(ProdutoRepository repositorioProdutos) {
        this.repositorioProdutos = repositorioProdutos;
    }

    public ResumoCarrinho construirResumo(List<SelecaoCarrinho> selecoes) {

        List<ItemCarrinho> itens = new ArrayList<>();

        // =========================
        // Monta os itens do carrinho
        // =========================
        for (SelecaoCarrinho selecao : selecoes) {
            Produto produto = repositorioProdutos.buscarPorId(selecao.getProdutoId())
                    .orElseThrow(
                            () -> new IllegalArgumentException("Produto não encontrado: " + selecao.getProdutoId()));

            itens.add(new ItemCarrinho(produto, selecao.getQuantidade()));
        }

        // =========================
        // Calcula subtotal
        // =========================
        BigDecimal subtotal = itens.stream()
                .map(ItemCarrinho::calcularSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal percentualDesconto = calcularPercentualDesconto(itens);
        BigDecimal valorDesconto = subtotal.multiply(percentualDesconto)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal total = subtotal.subtract(valorDesconto)
                .setScale(2, RoundingMode.HALF_UP);

        return new ResumoCarrinho(itens, subtotal, percentualDesconto, valorDesconto, total);
    }

    private BigDecimal calcularPercentualDesconto(List<ItemCarrinho> itens) {
        int quantidadeTotal = itens.stream()
                .mapToInt(ItemCarrinho::getQuantidade)
                .sum();

        BigDecimal descontoQuantidade;
        if (quantidadeTotal >= 4) {
            descontoQuantidade = BigDecimal.valueOf(10);
        } else if (quantidadeTotal == 3) {
            descontoQuantidade = BigDecimal.valueOf(7);
        } else if (quantidadeTotal == 2) {
            descontoQuantidade = BigDecimal.valueOf(5);
        } else {
            descontoQuantidade = BigDecimal.ZERO;
        }

        BigDecimal descontoCategoria = itens.stream()
                .map(item -> percentualPorCategoria(item.getProduto().getCategoria())
                        .multiply(BigDecimal.valueOf(item.getQuantidade())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal percentualTotal = descontoQuantidade.add(descontoCategoria);
        return percentualTotal.min(BigDecimal.valueOf(25));
    }

    private BigDecimal percentualPorCategoria(CategoriaProduto categoria) {
        return switch (categoria) {
            case CAPINHA -> BigDecimal.valueOf(3);
            case CARREGADOR -> BigDecimal.valueOf(5);
            case FONE -> BigDecimal.valueOf(3);
            case PELICULA -> BigDecimal.valueOf(2);
            case SUPORTE -> BigDecimal.valueOf(2);
        };
    }
}
