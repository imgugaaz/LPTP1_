import java.io.*;
import java.nio.file.*;
import java.util.*;

public class Main {
    // Variáveis de Entrada
    private static final Map<String, Integer> totalVotosPartidoNacional = new HashMap<>();
    private static int totalVotosNacional = 0;
    // Diretórios de entrada e saída
    private static final String dirDados = "ficheirosdat";
    private static final String dirResultados = "resultadosEleitorias";

    // Função main
    public static void main(String[] args) {
        Path diretorioDados = Paths.get(dirDados);
        Path diretorioResultados = Paths.get(dirResultados);

        try {
            if (!Files.exists(diretorioResultados)) {
                Files.createDirectories(diretorioResultados);
            }

            Files.newDirectoryStream(diretorioDados, "*.dat").forEach(entry -> {
                try {
                    VotosCirculoEleitoral votosCirculo = lerDados(entry.toString());
                    salvarResultados(votosCirculo, diretorioResultados);
                } catch (IOException | ClassNotFoundException e) {
                    e.printStackTrace();
                }
            });

            int votosValidosNacional = totalVotosNacional - totalVotosPartidoNacional.getOrDefault("Brancos", 0) - totalVotosPartidoNacional.getOrDefault("Nulos", 0);
            StringBuilder resultadoNacional = textoResultante("Resultados Nacionais", totalVotosPartidoNacional, totalVotosNacional, votosValidosNacional);
            System.out.println(resultadoNacional);
            Files.write(diretorioResultados.resolve("TotalNacional.txt"), resultadoNacional.toString().getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    // Função para ler ficheiros .dat
    public static VotosCirculoEleitoral lerDados(String nomeArquivo) throws IOException, ClassNotFoundException {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(nomeArquivo))) {
            return (VotosCirculoEleitoral) ois.readObject();
        }
    }
    // Função para calcular os votos totais por partido em um círculo eleitoral
    public static Map<String, Integer> calcularVotosTotais(Map<String, VotosConcelho> votosPorConcelho) {
        Map<String, Integer> totalVotosPartido = new HashMap<>();
        votosPorConcelho.values().forEach(vc ->
                vc.getVotosPorPartido().forEach((partido, votos) ->
                        totalVotosPartido.merge(partido, votos, Integer::sum)));
        return totalVotosPartido;
    }
    // Função para gerar o texto resultante com os resultados de um círculo eleitoral
    public static StringBuilder textoResultante(String nomeCirculo, Map<String, Integer> totalVotosPartido, int totalVotos, int votosValidos) {
        StringBuilder resultado = new StringBuilder();
        resultado.append(String.format("Nome do círculo: %s\n", nomeCirculo))
                .append(String.format("Nº de votantes: %d\n", totalVotos))
                .append(String.format("Nº de votos válidos: %d (%.2f%%)\n", votosValidos, 100.0 * votosValidos / totalVotos))
                .append(String.format("Nº de votos brancos: %d (%.2f%%)\n", totalVotosPartido.getOrDefault("Brancos", 0), 100.0 * totalVotosPartido.getOrDefault("Brancos", 0) / totalVotos))
                .append(String.format("Nº de votos nulos: %d (%.2f%%)\n", totalVotosPartido.getOrDefault("Nulos", 0), 100.0 * totalVotosPartido.getOrDefault("Nulos", 0) / totalVotos));

        // Organiza os partidos por ordem de percentagem de votos
        List<Map.Entry<String, Integer>> sortedEntries = new ArrayList<>(totalVotosPartido.entrySet());
        sortedEntries.sort((e1, e2) -> Double.compare((double)e2.getValue() / votosValidos, (double)e1.getValue() / votosValidos));

        for (Map.Entry<String, Integer> entry : sortedEntries) {
            String partido = entry.getKey();
            int votos = entry.getValue();
            if (!partido.equals("Brancos") && !partido.equals("Nulos")) {
                resultado.append(String.format("%s - %.2f%% (%d)\n", partido, 100.0 * votos / votosValidos, votos));
            }
        }

        return resultado;
    }
    // Função para salvar os resultados de um círculo eleitoral em um arquivo
    public static void salvarResultados(VotosCirculoEleitoral votosCirculo, Path diretorioResultados) throws IOException {
        Map<String, Integer> totalVotosPartido = calcularVotosTotais(votosCirculo.getVotosPorConcelho());
        int totalVotos = totalVotosPartido.values().stream().mapToInt(Integer::intValue).sum();
        totalVotosNacional += totalVotos;
        totalVotosPartido.forEach((partido, votos) -> totalVotosPartidoNacional.merge(partido, votos, Integer::sum));

        int votosBrancos = totalVotosPartido.getOrDefault("Brancos", 0);
        int votosNulos = totalVotosPartido.getOrDefault("Nulos", 0);
        int votosValidos = totalVotos - votosBrancos - votosNulos;
        StringBuilder resultado = textoResultante(votosCirculo.getNomeCirculo(), totalVotosPartido, totalVotos, votosValidos);
        Files.write(diretorioResultados.resolve(votosCirculo.getNomeCirculo() + ".txt"), resultado.toString().getBytes());
    }
}
