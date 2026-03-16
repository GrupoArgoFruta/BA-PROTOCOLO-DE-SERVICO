package br.com.argo.protocoloservico.controller;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;
import br.com.sankhya.extensions.actionbutton.Registro;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.core.JapeSession;
import br.com.sankhya.jape.core.JapeSession.SessionHandle;
import br.com.sankhya.jape.sql.NativeSql;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;

public class CopyAnexosSol implements AcaoRotinaJava {

    @Override
    public void doAction(ContextoAcao ctx) throws Exception {
        Registro[] linhas = ctx.getLinhas();
        
        // Boa prática: SessionHandle deve ser fechado no finally, embora o container geralmente gerencie isso.
        SessionHandle hnd = JapeSession.open();
        
        if (linhas.length == 0) {
            ctx.setMensagemRetorno("⚠ Nenhuma nota selecionada na tela.");
            return;
        }

        try {
            EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
            NativeSql nativeSql = new NativeSql(dwfFacade.getJdbcWrapper());
            // 1. Pega a Nota de Destino (onde o botão foi clicado) vai pegar o ultimo anexo somente
            BigDecimal nuNotaDestino = (BigDecimal) linhas[0].getCampo("NUNOTA");

            // 2. Pega a Nota de Origem (do parâmetro)
            Object paramVenda = ctx.getParam("NUNOTA_ORIGEM");

            if (paramVenda == null) {
                ctx.setMensagemRetorno("⚠ Parâmetro NUNOTA_ORIGEM não informado.");
                return;
            }

            BigDecimal nuNotaOrigem = new BigDecimal(paramVenda.toString());

            // Validação simples
            if (nuNotaDestino.equals(nuNotaOrigem)) {
                ctx.setMensagemRetorno("⚠ A nota de origem não pode ser a mesma de destino.");
                return;
            }

            Map<String, Object> ultimoAnexo = buscarUltimoAnexo(nativeSql, nuNotaOrigem);

            if (ultimoAnexo == null) {
                ctx.setMensagemRetorno("⚠ Nenhum anexo encontrado na nota de origem: " + nuNotaOrigem);
                return;
            }

            inserirAnexosNaNota(nuNotaDestino, ultimoAnexo);

            ctx.setMensagemRetorno(" Último anexo copiado com sucesso da nota "
                    + nuNotaOrigem + " para a nota " + nuNotaDestino + ".");

        } catch (Exception e) {
            e.printStackTrace();
            ctx.mostraErro("Erro ao copiar anexos: " + e.getMessage());
        } finally {
            JapeSession.close(hnd);
        }
    }

    // Método para buscar os anexos da origem
//    public List<Map<String, Object>> buscarAnexos(BigDecimal nuNota) throws Exception {
//        List<Map<String, Object>> resultList = new ArrayList<>();
//        // VERIFIQUE SE A INSTÂNCIA É REALMENTE "Anexo" (Geralmente é TGFANX ou TSIANX)
//        JapeWrapper itemDAO = JapeFactory.dao("Anexo");
//
//        // Assumindo que o campo de ligação é CODATA (Verifique se não é NUNOTA)
//        Collection<DynamicVO> itens = itemDAO.find("CODATA = ?", nuNota);
//
//        for (DynamicVO vo : itens) {
//            Map<String, Object> row = new HashMap<>();
//            row.put("DESCRICAO", vo.asString("DESCRICAO"));
//            row.put("TIPO", vo.asString("TIPO"));
//            row.put("ARQUIVO", vo.asString("ARQUIVO"));
//            row.put("CONTEUDO", vo.asBlob("CONTEUDO")); // Blob
//            resultList.add(row);
//        }
//        return resultList;
//    }
    private Map<String, Object> buscarUltimoAnexo(NativeSql nativeSql, BigDecimal nuNota) throws Exception {
        String sql = "SELECT DESCRICAO, TIPO, ARQUIVO, CONTEUDO FROM TSIATA " +
                "WHERE CODATA = :CODATA " +
                "AND DTINCLUSAO = (SELECT MAX(DTINCLUSAO) " +
                "FROM TSIATA " +
                "WHERE CODATA = :CODATA)";

        nativeSql.resetSqlBuf();
        nativeSql.appendSql(sql);
        nativeSql.setNamedParameter("CODATA", nuNota);

        ResultSet rs = nativeSql.executeQuery();

        if (rs.next()) {
            Map<String, Object> row = new HashMap<>();
            row.put("DESCRICAO", rs.getString("DESCRICAO"));
            row.put("TIPO", rs.getString("TIPO"));
            row.put("ARQUIVO", rs.getString("ARQUIVO"));
            row.put("CONTEUDO", rs.getBlob("CONTEUDO"));
            return row;
        }

        return null;
    }
    // Método para inserir os anexos no destino
//    public void inserirAnexosNaNota(BigDecimal nuNotaDestino, Map<String, Object> dadosItem) throws Exception {
//        JapeWrapper cabDAO = JapeFactory.dao("Anexo");
//        cabDAO.create()
//            .set("CODATA", nuNotaDestino) // Vincula a nova nota
//            .set("DESCRICAO", dadosItem.get("DESCRICAO"))
//            .set("TIPO", dadosItem.get("TIPO"))
//            .set("ARQUIVO", dadosItem.get("ARQUIVO"))
//            .set("CONTEUDO", dadosItem.get("CONTEUDO"))
//            .set("DTINCLUSAO", new java.sql.Timestamp(System.currentTimeMillis())) // Data de agora
//            .save();
//    }

    public void inserirAnexosNaNota(BigDecimal nuNotaDestino, Map<String, Object> dadosItem) throws Exception {
        // Converte Blob para byte[]
        byte[] conteudoBytes = null;
        Object conteudo = dadosItem.get("CONTEUDO");
        if (conteudo instanceof java.sql.Blob) {
            java.sql.Blob blob = (java.sql.Blob) conteudo;
            conteudoBytes = blob.getBytes(1, (int) blob.length());
        }

        JapeWrapper cabDAO = JapeFactory.dao("Anexo");
        cabDAO.create()
                .set("CODATA", nuNotaDestino)
                .set("DESCRICAO", dadosItem.get("DESCRICAO"))
                .set("TIPO", dadosItem.get("TIPO"))
                .set("ARQUIVO", dadosItem.get("ARQUIVO"))
                .set("CONTEUDO", conteudoBytes)
                .set("DTINCLUSAO", new java.sql.Timestamp(System.currentTimeMillis()))
                .save();
    }
}