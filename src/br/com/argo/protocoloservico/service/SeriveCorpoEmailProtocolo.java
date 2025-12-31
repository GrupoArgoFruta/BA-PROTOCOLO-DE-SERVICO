package br.com.argo.protocoloservico.service;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sankhya.util.BigDecimalUtil;
import com.sankhya.util.TimeUtils;

import br.com.argo.protocoloservico.modal.ModalProtocoloServico;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;
import br.com.sankhya.extensions.actionbutton.Registro;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.bmp.PersistentLocalEntity;
import br.com.sankhya.jape.core.JapeSession;
import br.com.sankhya.jape.core.JapeSession.SessionHandle;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.sql.NativeSql;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.vo.EntityVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.modelcore.auth.AuthenticationInfo;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;
import br.com.sankhya.ws.ServiceContext;

public class SeriveCorpoEmailProtocolo {
	String msg;
	ServiceEmailTeste servicoteste = new ServiceEmailTeste();
	ModalProtocoloServico modelo = new ModalProtocoloServico ();
	public void CorpoEmail(ContextoAcao ctx,String tabelaHtml) throws Exception {
	    JdbcWrapper jdbc = JapeFactory.getEntityFacade().getJdbcWrapper();
	    SessionHandle hnd = JapeSession.open();
	    NativeSql nativeSql = new NativeSql(jdbc);
	    EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
	    String usuarioLogadoNome = ((AuthenticationInfo) ServiceContext.getCurrent().getAutentication()).getUsuVO().getNOMEUSU();
	    BigDecimal usuarioLogadoID = ((AuthenticationInfo) ServiceContext.getCurrent().getAutentication()).getUserID();
	    Timestamp dataAtual = new Timestamp(new Date().getTime());
	    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
	    String dataHoraAtualFormatada = sdf.format(dataAtual);
	    String assunto = "Protocolo de solicitação - Serviço";
//	    String email = (String) ctx.getParam("EMAIL");
	    String email =  null;
	    // Construindo o valor a ser inserido no campo AD_PROTOCOLO
        String infProtocolo = "Usuário: " + usuarioLogadoNome + " | ID: " + usuarioLogadoID + " | Data: " + dataHoraAtualFormatada;
        try {
            Registro[] linhas = ctx.getLinhas();

            // Mapas
            Map<BigDecimal, List<byte[]>> anexosPorNota = new HashMap<>();
            Map<BigDecimal, List<String>> nomesPorNota = new HashMap<>();

            // --- Loop pelas Linhas ---
            for (Registro registro : linhas) {
                BigDecimal nUnico = (BigDecimal) registro.getCampo("NUNOTA");

                // Inicializa listas
                anexosPorNota.putIfAbsent(nUnico, new ArrayList<>());
                nomesPorNota.putIfAbsent(nUnico, new ArrayList<>());

                // Query buscando o ARQUIVO e o CONTEUDO
                ResultSet query = nativeSql.executeQuery(
                    "SELECT CODATA, CONTEUDO, DESCRICAO FROM TSIATA WHERE CODATA = " + nUnico
                );

                boolean encontrouAnexoValidoNestaNota = false; // Flag para controlar se achou algo nesta nota

                while (query.next()) {
                    byte[] conteudoAnexo = query.getBytes("CONTEUDO");
                    String nomeArquivo = query.getString("DESCRICAO");

                    // Verifica se o CONTEUDO realmente existe (não é nulo e tem tamanho)
                    if (conteudoAnexo != null && conteudoAnexo.length > 0) {
                        encontrouAnexoValidoNestaNota = true; // Marcou que achou!
                        
                        anexosPorNota.get(nUnico).add(conteudoAnexo);

                        // Tratamento do Nome
                        if (nomeArquivo == null || nomeArquivo.trim().isEmpty()) {
                            nomeArquivo = "Anexo_Nota_" + nUnico + ".pdf";
                        }
                        nomesPorNota.get(nUnico).add(nomeArquivo);
                    } else {
                        // BLOQUEIO 1: Existe registro na tabela, mas o binário está vazio
                        query.close(); // Fecha antes de sair
                        ctx.mostraErro("Erro na Nota " + nUnico + ": O anexo '" + nomeArquivo + "' existe mas o conteúdo está vazio. Verifique se foi salvo corretamente no Banco de Dados.");
                        return; // <--- ISSO PARA O PROCESSO IMEDIATAMENTE
                    }
                }
                query.close();

                // BLOQUEIO 2: Não encontrou nenhum anexo válido para a nota atual
                if (!encontrouAnexoValidoNestaNota) {
                    ctx.mostraErro("Erro: A nota " + nUnico + " não possui anexos. É obrigatório ter anexo para enviar o protocolo.");
                    return; // <--- ISSO PARA O PROCESSO IMEDIATAMENTE
      
                }
            }

            // --- 3. Montagem do HTML ---
            // Se chegou aqui, é porque TODAS as notas selecionadas têm anexos válidos
            StringBuilder descricoesHtml = new StringBuilder();
            
            descricoesHtml.append("<h3 style='color:#1e6533; margin-top: 25px; border-bottom: 1px solid #ccc; padding-bottom: 5px;'>")
                          .append(" Anexos Relacionados")
                          .append("</h3>");

            for (BigDecimal nota : anexosPorNota.keySet()) {
                descricoesHtml.append("<div style='background-color: #fcfcfc; border: 1px solid #e0e0e0; border-radius: 5px; padding: 15px; margin-bottom: 15px;'>");
                descricoesHtml.append("<div style='font-weight: bold; color: #1e6533; margin-bottom: 10px; font-size: 14px;'>")
                              .append("Pedido Nº: ").append(nota)
                              .append("</div>");
                descricoesHtml.append("<ul style='list-style-type: none; padding: 0; margin: 0;'>");

                List<String> nomes = nomesPorNota.get(nota);
                for (String nome : nomes) {
                    descricoesHtml.append("<li style='background-color: #fff; border-bottom: 1px solid #eee; padding: 8px; font-size: 13px; color: #555;'>")
                                  .append("<b>Arquivo:</b> ").append(nome)
                                  .append("</li>");
                }
                descricoesHtml.append("</ul>");
                descricoesHtml.append("</div>");
            }

            // Corpo do E-mail
            String mensagem = "<!DOCTYPE html>" +
                    "<html>" +
                    "<head>" +
                    "    <meta charset=\"utf-8\"/>" +
                    "    <style>" +
                    "        body { font-family: Arial, sans-serif; color: #333; }" +
                    "        .protocolo-box { background-color: #f8f9fa; border-left: 6px solid #1e6533; padding: 15px; margin: 20px 0; border-radius: 4px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }" +
                    "        .protocolo-item { margin: 5px 0; font-size: 14px; }" +
                    "        .label { font-weight: bold; color: #1e6533; }" +
                    "    </style>" +
                    "</head>" +
                    "<body>" +
                    "    <h2 style='color:#1e6533;'>Informações do Protocolo Serviço</h2>" +
                    "    <div class='protocolo-box'>" +
                    "        <div class='protocolo-item'><span class='label'>Usuário:</span> " + usuarioLogadoNome + "</div>" +
                    "        <div class='protocolo-item'><span class='label'>ID:</span> " + usuarioLogadoID + "</div>" +
                    "        <div class='protocolo-item'><span class='label'>Data:</span> " + dataHoraAtualFormatada + "</div>" +
                    "    </div>" +
                    "    <h2 style='color:#1e6533; margin-top: 30px;'>Detalhes do Pedido</h2>" +
                    "    <div style='overflow-x:auto;'>" +
                             tabelaHtml +
                             descricoesHtml.toString() +
                    "    </div>" +
                    "</body>" +
                    "</html>";

            // --- 4. Preparação e Envio ---
            List<byte[]> todosAnexos = new ArrayList<>();
            List<String> todosNomesArquivos = new ArrayList<>();

            for (BigDecimal nota : anexosPorNota.keySet()) {
                todosAnexos.addAll(anexosPorNota.get(nota));
                todosNomesArquivos.addAll(nomesPorNota.get(nota));
            }

            // Chama o método de envio (agora com certeza tem anexos)
            enviarEmailComAnexos(dwfFacade, ctx, todosAnexos, todosNomesArquivos, assunto, mensagem);

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Erro ao gerar o envio: " + e.getMessage(), e);
        } finally {
            JapeSession.close(hnd);
            JdbcWrapper.closeSession(jdbc);
            NativeSql.releaseResources(nativeSql);
        }
    }

	private void enviarEmailComAnexos(EntityFacade dwfEntityFacade, ContextoAcao contexto, 
	        List<byte[]> anexos, List<String> nomesArquivos, String assunto, String mensagem) throws Exception {
			BigDecimal codFila = null;
			BigDecimal nuAnexo = null;
			BigDecimal seq = null;
			JdbcWrapper jdbc = JapeFactory.getEntityFacade().getJdbcWrapper();

			NativeSql nativeSql = new NativeSql(jdbc);

			try {
				// Passo 1: Criação da Mensagem na MSDFilaMensagem para obter o CODFILA
				DynamicVO dynamicVO1 = (DynamicVO) dwfEntityFacade.getDefaultValueObjectInstance("MSDFilaMensagem");
				dynamicVO1.setProperty("ASSUNTO", assunto);
				dynamicVO1.setProperty("CODMSG", null);
				dynamicVO1.setProperty("DTENTRADA", TimeUtils.getNow());
				dynamicVO1.setProperty("STATUS", "Pendente");
				dynamicVO1.setProperty("CODCON", BigDecimal.ZERO);
				dynamicVO1.setProperty("TENTENVIO", BigDecimalUtil.valueOf(3));
				dynamicVO1.setProperty("MENSAGEM", mensagem.toCharArray());
				dynamicVO1.setProperty("TIPOENVIO", "E");
				dynamicVO1.setProperty("MAXTENTENVIO", BigDecimalUtil.valueOf(3));
				dynamicVO1.setProperty("EMAIL", "iranilde.moura@argofruta.com");
				dynamicVO1.setProperty("CODSMTP", null);
				dynamicVO1.setProperty("CODUSUREMET",contexto.getUsuarioLogado());
				dynamicVO1.setProperty("MIMETYPE", "text/html");
				PersistentLocalEntity createEntity = dwfEntityFacade.createEntity("MSDFilaMensagem", (EntityVO) dynamicVO1);
				codFila = ((DynamicVO) createEntity.getValueObject()).asBigDecimal("CODFILA");

				// Passo 2: Criação dos Anexos na AnexoMensagem para obter os NUANEXO
				// Anexo Invoice
				 for (int i = 0; i < anexos.size(); i++) {
				DynamicVO dynamicVO2Invoice = (DynamicVO) dwfEntityFacade.getDefaultValueObjectInstance("AnexoMensagem");
				dynamicVO2Invoice.setProperty("ANEXO", anexos.get(i));
				dynamicVO2Invoice.setProperty("NOMEARQUIVO", nomesArquivos.get(i) + ".pdf");
				dynamicVO2Invoice.setProperty("TIPO", "application/pdf");
				createEntity = dwfEntityFacade.createEntity("AnexoMensagem", (EntityVO) dynamicVO2Invoice);
				nuAnexo = ((DynamicVO) createEntity.getValueObject()).asBigDecimal("NUANEXO");
				 
				// Passo 3: Associação dos Anexos à Mensagem na TMDAXM
				// Anexo Invoice
				DynamicVO dynamicVO3Invoice = (DynamicVO) dwfEntityFacade.getDefaultValueObjectInstance("AnexoPorMensagem");
				dynamicVO3Invoice.setProperty("CODFILA", codFila);
				dynamicVO3Invoice.setProperty("NUANEXO", nuAnexo);
				dwfEntityFacade.createEntity("AnexoPorMensagem", (EntityVO) dynamicVO3Invoice);
				 }
				String querySeq  = ("SELECT MAX(SEQUENCIA) + 1 AS SEQUENCIA FROM TMDFMD WHERE CODFILA = " + codFila);
				ResultSet rsSeq = nativeSql.executeQuery(querySeq);
				
				while (rsSeq.next()) {
					seq = rsSeq.getBigDecimal("SEQUENCIA");
				}

			} catch (Exception e) {
				e.printStackTrace();
				msg = "Erro na criação da mensagem: " + e.getMessage();
				contexto.setMensagemRetorno(msg);
			}finally { 
				JdbcWrapper.closeSession(jdbc); 
				NativeSql.releaseResources(nativeSql);
			}
		}
}
