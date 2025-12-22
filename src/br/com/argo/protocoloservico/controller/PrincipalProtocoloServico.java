package br.com.argo.protocoloservico.controller;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import br.com.argo.protocoloservico.modal.ModalProtocoloServico;
import br.com.argo.protocoloservico.repository.AtualizarProtocolo;
import br.com.argo.protocoloservico.service.SeriveCorpoEmailProtocolo;
import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;
import br.com.sankhya.extensions.actionbutton.Registro;
import br.com.sankhya.jape.core.JapeSession;
import br.com.sankhya.jape.core.JapeSession.SessionHandle;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.sql.NativeSql;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.modelcore.auth.AuthenticationInfo;
import br.com.sankhya.ws.ServiceContext;

public class PrincipalProtocoloServico implements AcaoRotinaJava{
	//metodos auxiliares
	SeriveCorpoEmailProtocolo envioCorpo = new SeriveCorpoEmailProtocolo();
	AtualizarProtocolo atualizarProtocolo = new AtualizarProtocolo();
	ModalProtocoloServico modelo = new ModalProtocoloServico ();
	@Override
	public void doAction(ContextoAcao ctx) throws Exception {
		// TODO Auto-generated method stub
	    Registro[] linhas = ctx.getLinhas();
	    String usuarioLogadoNome = ((AuthenticationInfo) ServiceContext.getCurrent().getAutentication()).getUsuVO().getNOMEUSU();
	    BigDecimal usuarioLogadoID = ((AuthenticationInfo) ServiceContext.getCurrent().getAutentication()).getUserID();
	    Timestamp dataAtual = new Timestamp(new Date().getTime());
	    JdbcWrapper jdbc = JapeFactory.getEntityFacade().getJdbcWrapper();
		SessionHandle hnd = JapeSession.open();
		 // Criar um formatador para moeda
	    NumberFormat moedaFormat = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
	    List<String> numerosNotas = new ArrayList<>();
		try {
	        // Formatar data com hora e minuto
	        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
//	        SimpleDateFormat ven = new SimpleDateFormat("dd/MM/yyyy");
	        String dataHoraAtualFormatada = sdf.format(dataAtual);
	        
	        // Construir tabela consolidada de todas as linhas
	        StringBuilder tabelaHtml = new StringBuilder();
            tabelaHtml.append("<table style='border-collapse: collapse; width: 100%; font-family: Arial, sans-serif; font-size: 12px;'>")
                      .append("<tr style='background-color: #f2f2f2;'>")
                      .append("<th style='border: 1px solid #ddd; padding: 8px;'>USUÁRIO</th>")
                      .append("<th style='border: 1px solid #ddd; padding: 8px;'>DATA DE RECEBIMENTO</th>")
                      .append("<th style='border: 1px solid #ddd; padding: 8px;'>CÓDIGO EMPRESA </th>")
                      .append("<th style='border: 1px solid #ddd; padding: 8px;'>PEDIDO </th>") 
                      .append("<th style='border: 1px solid #ddd; padding: 8px;'>STATUS DO PEDIDO</th>")
                      .append("<th style='border: 1px solid #ddd; padding: 8px;'>Nº DOCUMENTO FISCAL</th>")
                      .append("<th style='border: 1px solid #ddd; padding: 8px;'>FORNECEDOR</th>")
                      .append("<th style='border: 1px solid #ddd; padding: 8px;'>VALOR TOTAL</th>")
                      .append("<th style='border: 1px solid #ddd; padding: 8px;'>DATA DE VENCIMENTO</th>")
                      .append("<th style='border: 1px solid #ddd; padding: 8px;'>RATEIO?</th>")
                      .append("</tr>");
            for (Registro registro : linhas) {
            	NativeSql nativeSql = new NativeSql(jdbc);
	        	String infoProtoservico = (String) registro.getCampo("AD_PROTSERVICO");
	        	String tipmov = (String) registro.getCampo("TIPMOV");
	        	BigDecimal codtop = (BigDecimal) registro.getCampo("CODTIPOPER");
	        	Date dthroperacao = (Date) registro.getCampo("DHTIPOPER");
	            BigDecimal codparc = (BigDecimal) registro.getCampo("CODPARC");
	            BigDecimal nUnico = (BigDecimal) registro.getCampo("NUNOTA");
	            BigDecimal nUmnota = (BigDecimal) registro.getCampo("NUMNOTA");
	            BigDecimal codcencus = (BigDecimal) registro.getCampo("CODCENCUS");
	            BigDecimal codnat = (BigDecimal) registro.getCampo("CODNAT");
	            BigDecimal codEmpresa = (BigDecimal) registro.getCampo("CODEMP");
//	            Timestamp dataenvio = (Timestamp) registro.getCampo("AD_DTPROCOLO");
	            String pedidoanual = (String) registro.getCampo("AD_PEDIDOANUAL");
	            // Construindo o valor a ser inserido no campo AD_PROTOCOLO
	            String infProtocolo = "Usuário: " + usuarioLogadoNome + " | ID: " + usuarioLogadoID + " | Data: " + dataHoraAtualFormatada;
	            String status = (String) registro.getCampo("STATUSNOTA");
	            String obs = (String) registro.getCampo("AD_OBS_PROTOCOLO");
	            String pendente  = (String) registro.getCampo("PENDENTE");
	            BigDecimal vlrTotal = (BigDecimal) registro.getCampo("VLRNOTA");
	            
	            
	         // Obtendo informações do parceiro
	            JapeWrapper parcDAO = JapeFactory.dao("Parceiro");
	            DynamicVO parcVO = parcDAO.findByPK(codparc);
	            String razaosocial = parcVO.asString("RAZAOSOCIAL");
	            String nomeparc = parcVO.asString("NOMEPARC");
	            String reabrirProtocolo = parcVO.asString("AD_ATIVOPROSERVI");
	            
				// VALIDAÇÕES VISUAIS
				// 1. Validação TOP (Pedido de Requisição - J)
				if (tipmov == null || !"J".equals(tipmov)) {
					modelo.tratarErro(ctx, "Movimento Inválido",
							"A nota <b>" + nUnico + "</b> não pode ser processada.<br>"
									+ "Apenas movimentos do tipo <b>Pedido de Requisição (J)</b> são permitidos.<br>"
									+ "Tipo atual: " + (tipmov == null ? "Nulo" : tipmov));
					return; // <--- OBRIGATÓRIO PARA PARAR A EXECUÇÃO
				}
				// 2. Validação Pendente
				if (!"S".equals(pendente)) {
					modelo.tratarErro(ctx, "Pendência Encontrada", "A nota <b>" + nUnico
							+ "</b> não pode ser processada.<br>" + "O status de pendente deve ser 'SIM'.");
					return;
				}
				// 3. Validação Status
				if (!"L".equals(status)) {
					modelo.tratarErro(ctx, "Status Inválido",
							"A nota <b>" + nUnico + "</b> não pode ser processada.<br>"
									+ "O status da nota deve ser <b>'Liberado' (L)</b>.<br>" + "Status atual: "
									+ status);
					return;
				}
				// 4. Validação Reenvio
				if (infoProtoservico != null && !infoProtoservico.trim().isEmpty()) {
					if (!"S".equals(reabrirProtocolo)) {
						modelo.tratarErro(ctx, "Protocolo Já Gerado",
								"O registro <b>" + nUnico + "</b> já foi enviado anteriormente.<br>"
										+ "Não é permitido gerar um novo protocolo sem autorização de reabertura.");
						return;
					}
				}
			
                // -------------------------------
	         // Lógica de processamento
	            numerosNotas.add(nUnico.toString());
	            String descricaoAnexos = obterDescricoesAnexos(nativeSql, nUnico);
	            String datasVencimento = obterDatasVencimento(nativeSql, nUnico);
	            String existeRateio = verificarRateio(nUnico);
	            
	            
          
	            
	            
	            atualizarProtocolo.atualizarInfoProtocolo(nUnico, usuarioLogadoNome, usuarioLogadoID, dataHoraAtualFormatada);
                atualizarProtocolo.DataEnvioProtocolo(nUnico, dataAtual);
	 
                String mensagemStatus = obterMensagemStatus(status);
                String valorFormatado = (vlrTotal != null) ? moedaFormat.format(vlrTotal) : "R$ 0,00";

                tabelaHtml.append("<tr>")
                          .append("<td style='border: 1px solid #ddd; padding: 8px;'>").append(usuarioLogadoNome).append("</td>")
                          .append("<td style='border: 1px solid #ddd; padding: 8px;'>").append(dataHoraAtualFormatada).append("</td>")
                          .append("<td style='border: 1px solid #ddd; padding: 8px;'>").append(codEmpresa).append("</td>")
                          .append("<td style='border: 1px solid #ddd; padding: 8px;'>").append(nUnico).append("</td>")
                          .append("<td style='border: 1px solid #ddd; padding: 8px;'>").append(mensagemStatus).append("</td>")
                          .append("<td style='border: 1px solid #ddd; padding: 8px;'>").append(descricaoAnexos).append("</td>")
                          .append("<td style='border: 1px solid #ddd; padding: 8px;'>").append(razaosocial).append("</td>")
                          .append("<td style='border: 1px solid #ddd; padding: 8px; text-align: right;'>").append(valorFormatado).append("</td>")
                          .append("<td style='border: 1px solid #ddd; padding: 8px;'>").append(datasVencimento.length() > 0 ? datasVencimento.toString() : "N/A").append("</td>")
                          .append("<td style='border: 1px solid #ddd; padding: 8px;'>").append(existeRateio).append("</td>")
                          .append("</tr>");
            }
            tabelaHtml.append("</table>");

            // Envia Email
            envioCorpo.CorpoEmail(ctx, tabelaHtml.toString());

            // Mensagem de Retorno na Tela
            StringBuilder mensagemSucesso = new StringBuilder();
            mensagemSucesso.append("<!DOCTYPE html>")
                           .append("<html>")
                           .append("<body>")
                           .append("<p align='center'><img src='https://argofruta.com/wp-content/uploads/2021/05/Logo-text-green.png' width='110' height='65'></p>")
                           .append("<p style='font-family:courier; color:Black; text-align:center;'>")
                           .append("<b>Processo concluído com sucesso!</b><br><br>")
                           .append("Os Nº único processados foram:<br>")
                           // Exibe a lista separada por vírgula
                           .append("<b>").append(String.join(", ", numerosNotas)).append("</b>") 
                           .append("</p>")
                           .append("<hr>")
                           .append("</body>")
                           .append("</html>");

            ctx.setMensagemRetorno(mensagemSucesso.toString());
			
		} catch (Exception e) {
			// TODO: handle exception
			ctx.mostraErro("Erro ao processar a ação botão protocolo serviço : " + e.getMessage());
		}
	}
	// Métodos auxiliares
	private String obterMensagemStatus(String status) {
	    if ("A".equals(status)) {
	        return "Pedido não confirmado";
	    } else if ("L".equals(status)) {
	        return "Pedido confirmado";
	    }
	    return "Status desconhecido.";
	}
	
	private String obterDescricoesAnexos(NativeSql nativeSql, BigDecimal nUnico) throws Exception {
	    StringBuilder descricoes = new StringBuilder();
	    ResultSet query = nativeSql.executeQuery(
	        "SELECT DESCRICAO FROM TSIATA WHERE CODATA = " + nUnico
	    );
	    
	    while (query.next()) {
	        String descricao = query.getString("DESCRICAO");
	        if (descricao != null && !descricao.isEmpty()) {
	            if (descricoes.length() > 0) {
	                descricoes.append(", ");
	            }
	            descricoes.append(descricao);
	        }
	    }
	    
	    return descricoes.length() > 0 ? descricoes.toString() : "Sem descrição";
	}
	
	private String obterDatasVencimento(NativeSql nativeSql, BigDecimal nUnico) throws Exception {
	    StringBuilder datasVencimento = new StringBuilder();
	    SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
	    SessionHandle hnd = JapeSession.open();
	    ResultSet query = null;
	    try {
	        query = nativeSql.executeQuery("SELECT DTVENC FROM TGFFIN WHERE NUNOTA = " + nUnico);
	        while (query.next()) {
	            Date dtVencimento = query.getDate("DTVENC");
	            if (dtVencimento != null) {
	                if (datasVencimento.length() > 0) {
	                    datasVencimento.append(", ");
	                }
	                datasVencimento.append(dateFormat.format(dtVencimento));
	            }
	        }
	    } catch (Exception e) {
	        System.err.println("Erro ao obter datas de vencimento: " + e.getMessage());
	        throw new RuntimeException("Erro ao obter datas de vencimento.", e);
	    } finally {
	    	JapeSession.close(hnd);
	    }

	    return datasVencimento.length() > 0 ? datasVencimento.toString() : "N/A";
	}
	
	private String verificarRateio(BigDecimal nUnico) throws Exception {
		    JapeWrapper rateioDAO = JapeFactory.dao("RateioRecDesp");
		    boolean existeRateio = rateioDAO.findOne("NUFIN = ?", nUnico) != null;
		    return existeRateio ? "SIM" : "NÃO";
		}


}



// Obtendo informações do TIPO DE OPERAÇÃO
//JapeWrapper topDAO = JapeFactory.dao("TipoOperacao");
//DynamicVO topVO = topDAO.findByPK(codtop,dthroperacao);
//String grupo = topVO.asString("GRUPO");