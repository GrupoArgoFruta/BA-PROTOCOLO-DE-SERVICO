package br.com.argo.protocoloservico.repository;

import java.math.BigDecimal;
import java.sql.Timestamp;

import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.core.JapeSession;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.sql.NativeSql;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;

public class AtualizarProtocolo {
	 public void atualizarInfoProtocolo(BigDecimal nUnico, String usuarioLogadoNome, BigDecimal usuarioLogadoID, String dataAtual) throws Exception {
	        JapeSession.SessionHandle hnd = null;
	        JdbcWrapper jdbc = null;
	        NativeSql query = null;
	        try {
	            // Montando a query de atualização
	            String update = "UPDATE TGFCAB SET AD_PROTSERVICO = :AD_PROTSERVICO WHERE NUNOTA = :NUNOTA";

	            hnd = JapeSession.open();
	            hnd.setCanTimeout(false);
	            hnd.setFindersMaxRows(-1);
	            EntityFacade entity = EntityFacadeFactory.getDWFFacade();
	            jdbc = entity.getJdbcWrapper();
	            jdbc.openSession();

	            query = new NativeSql(jdbc);
	            query.appendSql(update);

	            // Construindo o valor a ser inserido no campo AD_PROTOCOLO
	            String novoProtocolo = "Usuario: " + usuarioLogadoNome + " | ID: " + usuarioLogadoID + " | Data: " + dataAtual;

	            // Definindo os parâmetros nomeados
	            query.setNamedParameter("AD_PROTSERVICO", novoProtocolo);
	            query.setNamedParameter("NUNOTA", nUnico);

	            query.executeUpdate(); // Executando o update
	        } catch (Exception e) {
	            e.printStackTrace();
	            throw new Exception("Erro ao executar a atualização atualizarInfoProtocolo: " + e.getMessage());
	        } finally {
	            JapeSession.close(hnd);
	            JdbcWrapper.closeSession(jdbc);
	            NativeSql.releaseResources(query);
	        }
	    }
	 public void DataEnvioProtocolo(BigDecimal nUnico, Timestamp dataAtual )throws Exception {
		 JapeSession.SessionHandle hnd = null;
	     JdbcWrapper jdbc = null;
	     NativeSql query = null;
	     try {
	    	// Montando a query de atualização
	            String update = "UPDATE TGFCAB SET AD_DTPROCOSERVI = :AD_DTPROCOSERVI WHERE NUNOTA = :NUNOTA";
	            hnd = JapeSession.open();
	            hnd.setCanTimeout(false);
	            hnd.setFindersMaxRows(-1);
	            EntityFacade entity = EntityFacadeFactory.getDWFFacade();
	            jdbc = entity.getJdbcWrapper();
	            jdbc.openSession();

	            query = new NativeSql(jdbc);
	            query.appendSql(update);
	            
	         // Definindo os parâmetros nomeados
	            query.setNamedParameter("AD_DTPROCOSERVI", dataAtual);
	            query.setNamedParameter("NUNOTA", nUnico);
	            
	            query.executeUpdate(); // Executando o update
			
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
            throw new Exception("Erro ao executar a atualização DataEnvioProtocolo: " + e.getMessage());
		} finally {
            JapeSession.close(hnd);
            JdbcWrapper.closeSession(jdbc);
            NativeSql.releaseResources(query);
        }
		 
		 
	 }

}
