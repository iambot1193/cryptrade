# CryptRade

Simulador de trading de criptomoedas. API REST em Kotlin + Spring Boot para compra/venda simulada com saldo virtual, posicoes e historico de ordens, usando preco real de mercado via CoinGecko.

## Stack

- Kotlin + Spring Boot 4 (Web MVC, Data JPA, Validation)
- H2 em memoria
- Gradle (Kotlin DSL)
- Preco de mercado: [CoinGecko API](https://www.coingecko.com/en/api) (publica, sem chave)

## Rodando

```bash
./gradlew bootRun
```

App sobe em `http://localhost:8080`. Console H2 em `/h2-console` (JDBC URL `jdbc:h2:mem:cryptrade`).

Carteira comeca com saldo virtual de 100000.00 (configuravel em `application.yml`).

## Endpoints

| Metodo | Rota | Descricao |
|---|---|---|
| GET | `/api/prices/{symbol}` | Preco atual (symbol = id CoinGecko, ex: `bitcoin`, `ethereum`) |
| POST | `/api/orders` | Executa ordem simulada `{ "symbol": "bitcoin", "side": "BUY", "quantity": 0.1 }` |
| GET | `/api/orders` | Historico de ordens |
| GET | `/api/portfolio` | Saldo, posicoes abertas e equity total |

## Exemplo

```bash
curl -X POST localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{"symbol":"bitcoin","side":"BUY","quantity":0.1}'

curl localhost:8080/api/portfolio
```

## Testes

```bash
./gradlew test
```
