package br.com.cabal.proc;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ArquivoServices {


    public File[] listarArquivos(String diretorio, String prefixo, String extensao) throws IOException {
        File pasta = new File(diretorio);
        if (!pasta.isDirectory()) {
            throw new IllegalArgumentException("O caminho especificado nao eh um diretorio valido.");
        }
        File[] arquivos = pasta.listFiles((dir, nome) -> nome.endsWith(extensao) && nome.startsWith(prefixo) && new File(dir, nome).isFile());
        
        return arquivos;
    }
    
    public Dto obterRegistros(File arquivo, String linhaContadora, String registroTipoReader, String registroTipoTrailler) throws Exception {
    	Dto dto = new Dto();
    	ArrayList<String> linhas = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(arquivo))) {
            String linha;
            while ((linha = br.readLine()) != null) {
            	if (linha.startsWith(registroTipoReader)) {
            		dto.setReader(linha);
            	}else if (linha.startsWith(registroTipoTrailler)){
            		dto.setTrailler(linha);
            	}else {
                	linhas.add(linha);
            	}
            	if(linha.startsWith(linhaContadora)) {
            		dto.setTotalDeBlocos(dto.getTotalDeBlocos() + 1);
            	}
            }
        } catch (IOException e) {
            throw new Exception("Erro ao ler o arquivo: " + e);            
        }
        
        if (dto.getReader().isEmpty() || dto.getTrailler().isEmpty() || dto.getTotalDeBlocos() == 0) {
            throw new Exception("O está imcompleto ou vazio. Arquivo: " + arquivo.getName() + " " + dto);
        }
        dto.setLinhas(linhas);
		return dto;
    }

    public List<String>[] dividirArq(List<String> linhasDoArquivo, String paramPrefixoBloco, int divisorInteger) {
        List<String>[] arrayDeListas = new ArrayList[divisorInteger];
        
        for (int i = 0; i < divisorInteger; i++) {
            arrayDeListas[i] = new ArrayList<>();
        }
        
        int index = 0;
        while (!linhasDoArquivo.isEmpty()) {
            List<String> linhasDoBloco = obtemProximoBloco(linhasDoArquivo, paramPrefixoBloco);
            arrayDeListas[index].addAll(linhasDoBloco);
            index = (index + 1) % divisorInteger; 
        }
        return arrayDeListas;
    }
    
    private List<String> obtemProximoBloco(List<String> linhas, String paramPrefixoBloco) {
        List<String> linhasDoBloco = new ArrayList<>();
        List<String> copiaLinhas = new ArrayList<>(linhas);

        for (String linha : copiaLinhas) {
            if (isBloco(paramPrefixoBloco, linha)) {
                linhasDoBloco.add(linha);
                linhas.remove(linha); 
                break;
            }
            linhasDoBloco.add(linha);
            linhas.remove(linha); 
        }
        return linhasDoBloco;
    }


	private boolean isBloco(String paramPrefixoBloco, String linha) {
		return linha.startsWith(paramPrefixoBloco);
	}
	
    public void gerarArquivo(String caminhoArquivo, String novoReadder, String novoTrailler, List<String> linhas) throws IOException {
    	if (linhas.isEmpty()) {return;}
    	
        File arquivo = new File(caminhoArquivo);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(arquivo))) {
        	 writer.write(novoReadder);
             writer.newLine();
            for (String linha : linhas) {
                writer.write(linha);
                writer.newLine();
            }
            writer.write(novoTrailler);
        }
    }   
}
