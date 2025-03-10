package br.com.proc;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import br.com.bc.bandeiras.visa.ctf.TCS;
import br.com.conf.Param;
import br.com.core.Formata;
import br.com.core.Prompt;

public class ProcCTFParalelo {
	private static Integer numThreads;
	private static String user;
	private static String pwd;
	private static String base;
	private static String idInterf;
	private static String sid;
	private static String driver;
	private static String varAux;     
	private static String m_sDirErro;            // diretório de erros
	private static String m_sDirTmp;             // diretório temporário
	private static String m_sDirEntrada;         // diretório com os arquivos a processar
	private static String m_sDirProcessados;     // diretório com os arquivos já processados
	private static String m_sDirGerados;         // diretório com os arquivos gerados
	private static String m_sExtArq;             // máscara dos arquivos a processar
	private static String m_sTipoArq; 
	private static String m_sPrefixoCabecalho; 
	private static String m_sPrefixoRodape; 
	private static String m_sTipoArqParalelo; 
	private static Prompt m_console;
	
	private static final String DIR_RAIZ                      = "dir.raiz";
	private static final String DIR_TMP                       = "dir.tmp";
	private static final String DIR_ERRO                      = "dir.erro";
	private static final String DIR_ENTRADA                   = "dir.entrada";
	private static final String DIR_PROCESSADOS               = "dir.processados";
	private static final String DIR_GERADOS                   = "dir.gerados";
	private static final String EXTENSAO_ARQ                  = "ext.arq";
	private static final String TIPO_ARQ                      = "arq.tipo";
	private static final String TIPO_ARQ_PARALELO             = "arq.tipo.paralelo";
	private static final String BIN                           = "bin";

	public static void main(String[] args) throws Exception {
		numThreads = Integer.parseInt(args[0]);
		user       = args[1];
		pwd        = args[2];
		base       = args[3];
		idInterf   = args[4];
		varAux     = args[5];

		obterDadosDoAplicativo(idInterf);
		validarParametros();
	
		dividirAruivoOriginal();
		processar();	
	}

	private static void obterDadosDoAplicativo(String sAplicativo) throws Exception {
		String dirRaiz = new String();
		Param objParam = new Param(sAplicativo);

		driver                    = objParam.getParam("driver");
		sid                       = objParam.getParam("metodo"); // + base

		m_sPrefixoCabecalho       = TCS.TC_90_CABECALHO + objParam.getParam(BIN);
		m_sPrefixoRodape          = TCS.TC_92_RODAPE_ARQUIVO + objParam.getParam(BIN);
		m_sExtArq                 = objParam.getParam(EXTENSAO_ARQ);
		m_sTipoArq                = objParam.getParam(TIPO_ARQ);
		m_sTipoArqParalelo        = objParam.getParam(TIPO_ARQ_PARALELO);
		dirRaiz                   = objParam.getParam(DIR_RAIZ).equals("vazio") ? " " : objParam.getParam(DIR_RAIZ);
		dirRaiz                   = System.getProperty(dirRaiz) == null ? dirRaiz : System.getProperty(dirRaiz);
		if (dirRaiz != null)
			dirRaiz = dirRaiz.trim();

		m_sDirTmp                 = dirRaiz + objParam.getParam(DIR_TMP);
		m_sDirErro                = dirRaiz + objParam.getParam(DIR_ERRO);
		m_sDirEntrada             = dirRaiz + objParam.getParam(DIR_ENTRADA);
		m_sDirProcessados         = dirRaiz + objParam.getParam(DIR_PROCESSADOS);
		m_sDirGerados             = dirRaiz + objParam.getParam(DIR_GERADOS);

		m_console = new Prompt();
		if (m_console != null) {
			m_console.escrevaln("tipo arq       : " + m_sTipoArq);
			m_console.escrevaln("extensão arq   : " + m_sExtArq);
			m_console.escrevaln("DIR_RAIZ       : " + DIR_RAIZ);
			m_console.escrevaln("dir raiz       : " + dirRaiz);
			m_console.escrevaln("dir erro       : " + m_sDirErro);
			m_console.escrevaln("dir tmp        : " + m_sDirTmp);
			m_console.escrevaln("dir entrada    : " + m_sDirEntrada);
			m_console.escrevaln("dir processados: " + m_sDirProcessados);
			m_console.escrevaln("dir gerados    : " + m_sDirGerados);
		}
	}

	private static void emiteRetorno(int contOk, int contErro, int contTotal) {
		m_console.escrevaln("qtd ok   : " + contOk);
		m_console.escrevaln("qtd erro : " + contErro);
		m_console.escrevaln("qtd total: " + contTotal);

		if (contTotal == 0) {
			m_console.escrevaln("============================================================");
			m_console.escrevaln("Não foi encontrado arquivo para processamento");
			m_console.escrevaln("============================================================");
			System.exit(1);
		} else if (contErro == 0) {
			m_console.escrevaln("Processamento de arquivos " + idInterf + " OK.");
		} else {
			m_console.escrevaln("Operador, ATENÇÃO: alguns arquivos foram processados com erro !!!");
			m_console.escrevaln("Obtenha autorização de SISTEMAS antes de executar o próximo processo !!!");
			System.exit(1);
		}
	}
	
	private static void moverArquivo(File arquivoOrigem, File arquivoDestino) {
        try {
            System.out.println("Movendo " + arquivoOrigem.getCanonicalPath() + " para " + arquivoDestino.getCanonicalPath() + " ...");
            if (!arquivoOrigem.renameTo(arquivoDestino)) {
                System.out.println("ATENÇÃO !!! Não consegui mover o arquivo.");
                System.out.println("            Operador verificar junto ao remetente do arquivo(" + arquivoOrigem.getName() + "), pois este encontra-se com problemas.");
                System.out.println("            O Processamento continuará normalmente.");
            }
        } catch (Exception e) {
            System.out.println("Erro ao mover o arquivo: " + e.getMessage());
        }
    }
	
	private static void validarParametros() {
		isParametroValido("numThreads", numThreads);
        isParametroValido("user", user);
        isParametroValido("pwd", pwd);
        isParametroValido("base", base);
        isParametroValido("idInterf", idInterf);
        isParametroValido("sid", sid);
        isParametroValido("driver", driver);
        isParametroValido("varAux", varAux);
        isParametroValido("m_sDirErro", m_sDirErro);
        isParametroValido("m_sDirTmp", m_sDirTmp);
        isParametroValido("m_sDirEntrada", m_sDirEntrada);
        isParametroValido("m_sDirProcessados", m_sDirProcessados);
        isParametroValido("m_sDirGerados", m_sDirGerados);
        isParametroValido("m_sExtArq", m_sExtArq);
        isParametroValido("m_sTipoArq", m_sTipoArq);
        isParametroValido("m_sPrefixoCabecalho", m_sPrefixoCabecalho);
        isParametroValido("m_sPrefixoRodape", m_sPrefixoRodape);
        isParametroValido("m_sTipoArqParalelo", m_sTipoArqParalelo);
    }
	
	private static void dividirAruivoOriginal() throws Exception {
		ArquivoServices arquivoServices = new ArquivoServices();
		File fDirProcessados            = new File(m_sDirProcessados);
		File fProcessado                = null;
		Date dataHoje                   = new Date();
   	 	System.out.println(mostrarDataHoraAtual() + " Iniciando a divisao do arquivo original.");
   	 	
		try {
	            File[] arquivos = arquivoServices.listarArquivos(m_sDirEntrada, m_sTipoArq, m_sExtArq); 
	            
	            for (File file : arquivos) {
	            	 Dto dto = arquivoServices.obterRegistros(file, String.valueOf(TCS.TC_91_RODAPE_LOTE), m_sPrefixoCabecalho, m_sPrefixoRodape);
           		     String readder = dto.getReader();

	            	 List<String>[] arquivoDividido = arquivoServices.dividirArq(dto.getLinhas(), String.valueOf(TCS.TC_91_RODAPE_LOTE), numThreads);
	            	 int sequenciaDaMascara = 1;
	            	 for (List<String> lista : arquivoDividido) {
	                     String caminhoArquivo = m_sDirEntrada + "/"+ m_sTipoArqParalelo + sequenciaDaMascara + "." + file.getName();

	                     arquivoServices.gerarArquivo(caminhoArquivo, readder, dto.getTrailler(), lista);
	                     readder = incrementaSequencial(readder);
	                     sequenciaDaMascara++;
					}
	 				fProcessado = new File(fDirProcessados, file.getName() + "." + Formata.getDataS(dataHoje, "yyyyMMddHHmm"));
	     			//moverArquivo(file, fProcessado);
	            }
	        } catch (Exception e) {
	        	System.exit(1);
	            throw new Exception("Erro ao fracionar o arquivo original", e);            
	        }
	}
	
	private static String incrementaSequencial(String readder) {
		String sequenciaDoReadder = readder.substring(76, 79);
        int seq = Integer.parseInt(sequenciaDoReadder);
        String novoSeq = String.format("%03d", seq + 1);
        return readder.substring(0, 76) + novoSeq + readder.substring(79);
	}

	private static void processar() throws IOException, InterruptedException {
		File fDirProcessados            = new File(m_sDirProcessados); // diretório de saida dos processados ok
		File fDirErro                   = new File(m_sDirErro);        // diretório de saida dos processados com erro
		File fProcessado                = null;
		Date dataHoje                   = new Date();
		int contadorOk                  = 0;
		int contadorErro                = 0;
		int total                       = 0;
		List<ResultadoProc> resultados  = new ArrayList<ResultadoProc>();
		ArquivoServices arquivoServices = new ArquivoServices();
		
		
		File[] arquivos = arquivoServices.listarArquivos(m_sDirEntrada, m_sTipoArqParalelo, m_sExtArq);
		total = arquivos.length;
		ExecutorService executor = Executors.newFixedThreadPool(numThreads);
		
		for (File file : arquivos) {
			executor.submit(new ExecutorBat(varAux, file, user, pwd, driver, sid, resultados));
		}
		
		while(resultados.size() < total) {
			Thread.sleep(1000);
		} 
 
		for (ResultadoProc item : resultados) {
			if (item.isSucesso()) {
				fProcessado = new File(fDirProcessados, item.getFile().getName() + "." + Formata.getDataS(dataHoje, "yyyyMMddHHmm"));
				contadorOk += 1;
			} else {
				fProcessado = new File(fDirErro, "erro." + item.getFile().getName() + "." + Formata.getDataS(dataHoje, "yyyyMMddHHmm"));
				contadorErro += 1;
			}
			moverArquivo(item.getFile(), fProcessado);
		}
		executor.shutdown();
		emiteRetorno(contadorOk, contadorErro, total);
	}
	
	private static String mostrarDataHoraAtual() {
	    LocalDateTime dataHoraAtual = LocalDateTime.now();
	    DateTimeFormatter formato = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss:SSS");
	    return dataHoraAtual.format(formato);
	}
	private static void isParametroValido(String nomeParametro, String valor) {
		if (valor == null || valor.isBlank()) {
			throw new IllegalArgumentException("Parâmetro " + nomeParametro + " invalido.");
		}
	}

	private static void isParametroValido(String nomeParametro, Integer valor) {
		if (valor == null || valor == 0 || valor > 40) {
			throw new IllegalArgumentException("Parâmetro " + nomeParametro + " invalido.");
		}
	}

}
