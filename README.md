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

## Estrutura do projeto

```
src/main/kotlin/com/felipelopes/cryptrade/
├── CryptradeApplication.kt   # entry point
├── config/
│   └── RestClientConfig.kt   # bean do RestClient p/ CoinGecko
├── controller/                # camada HTTP, sem regra de negocio
│   ├── OrderController.kt     # POST/GET /api/orders
│   ├── PortfolioController.kt # GET /api/portfolio
│   └── PriceController.kt     # GET /api/prices/{symbol}
├── service/
│   ├── TradingService.kt      # regra de negocio: valida saldo/posicao,
│   │                           # atualiza wallet, media de preco, grava ordem
│   ├── PriceService.kt        # busca preco atual na CoinGecko
│   └── WalletInitializer.kt   # seed do saldo inicial no boot (CommandLineRunner)
├── domain/                    # entidades JPA
│   ├── Wallet.kt               # saldo em caixa (linha unica, id fixo = 1)
│   ├── Position.kt             # posicao aberta por symbol (qtd + preco medio)
│   ├── Order.kt                # ordem executada (historico, tabela "orders")
│   └── OrderSide.kt            # enum BUY/SELL
├── repository/                 # Spring Data JPA (uma interface por entidade)
├── dto/                        # request/response, isolam entidade da API
│   ├── OrderDtos.kt
│   └── PortfolioDtos.kt
└── exception/
    ├── TradingExceptions.kt        # InsufficientFunds/Position, UnknownSymbol
    └── GlobalExceptionHandler.kt   # @RestControllerAdvice -> HTTP status certo
```

Fluxo de uma ordem: `Controller` valida DTO (`@Valid`) -> `TradingService.placeOrder`
busca preco (`PriceService`), confere saldo/posicao, atualiza `Wallet` e `Position`,
grava `Order` -> `GlobalExceptionHandler` traduz exceptions de negocio em HTTP 409/404.

`symbol` usa o id da CoinGecko direto (`bitcoin`, `ethereum`, ...) — sem tabela de
mapeamento ticker->id. Se quiser aceitar `BTC`/`ETH`, e o proximo ponto a adicionar.
