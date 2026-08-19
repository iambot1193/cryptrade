# CryptRade

[![CI](https://github.com/iambot1193/cryptrade/actions/workflows/ci.yml/badge.svg)](https://github.com/iambot1193/cryptrade/actions/workflows/ci.yml)

Simulador de trading de criptomoedas. API REST em Kotlin + Spring Boot para compra e venda
simulada com saldo virtual, posições e histórico de ordens, usando preço real de mercado
via CoinGecko.

## Status

**v0 — esqueleto.** Build e testes verdes, camadas separadas, migração de schema versionada,
CI em cada push. A execução ponta a ponta contra Postgres real ainda não foi validada.

| Versão | Escopo |
|---|---|
| **v0** (atual) | Esqueleto: camadas, CI, schema versionado, testes de regra de negócio |
| v0.5 | Backend rodando contra Postgres, migração exercitada em teste |
| v1.0 | Back + front + Docker, demo reproduzível em um comando |

## Stack

- Kotlin + Spring Boot 4 (Web MVC, Data JPA, Validation)
- Postgres + Flyway (H2 em memória apenas no perfil de teste)
- Gradle (Kotlin DSL)
- Preço de mercado: [CoinGecko API](https://www.coingecko.com/en/api) (pública, sem chave)

### Perfis

| Perfil | Fonte de preço |
|---|---|
| padrão | `CoinGeckoPriceProvider` — preço real de mercado |
| `test`, `demo` | `FakePriceProvider` — preço determinístico, sem rede |

O perfil `demo` existe para que CI e demonstração não dependam da rede nem do rate limit
da CoinGecko.

## Build e testes

```bash
./gradlew build
```

Os testes rodam no perfil `test`: H2 em memória, preço determinístico, sem chamada externa.

## Executando

Requer Postgres. Com Docker:

```bash
cp .env.example .env
docker compose up --build
```

A aplicação sobe em `http://localhost:8080` e o Postgres em `localhost:5432`.

> Ainda não validado ponta a ponta — é o objetivo da v0.5.

A carteira começa com saldo virtual de 100000.00 (configurável em `application.yml`).

## Endpoints

| Método | Rota | Descrição |
|---|---|---|
| GET | `/api/prices/{symbol}` | Preço atual (symbol = id CoinGecko, ex: `bitcoin`, `ethereum`) |
| POST | `/api/orders` | Executa ordem simulada `{ "symbol": "bitcoin", "side": "BUY", "quantity": 0.1 }` |
| GET | `/api/orders` | Histórico de ordens |
| GET | `/api/portfolio` | Saldo, posições abertas e equity total |

## Exemplo

```bash
curl -X POST localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{"symbol":"bitcoin","side":"BUY","quantity":0.1}'

curl localhost:8080/api/portfolio
```

## Estrutura do projeto

```
src/main/kotlin/com/felipelopes/cryptrade/
├── CryptradeApplication.kt        # entry point
├── config/
│   └── RestClientConfig.kt        # bean do RestClient para a CoinGecko
├── controller/                    # camada HTTP, sem regra de negócio
│   ├── OrderController.kt         # POST/GET /api/orders
│   ├── PortfolioController.kt     # GET /api/portfolio
│   └── PriceController.kt         # GET /api/prices/{symbol}
├── service/
│   ├── TradingService.kt          # regra de negócio: valida saldo e posição,
│   │                              # atualiza carteira, preço médio, grava ordem
│   ├── PriceService.kt            # delega para o PriceProvider ativo
│   ├── PriceProvider.kt           # interface: fonte de preço
│   ├── CoinGeckoPriceProvider.kt  # preço real (perfil padrão)
│   ├── FakePriceProvider.kt       # preço determinístico (perfis test e demo)
│   └── WalletInitializer.kt       # semeia o saldo inicial no boot
├── domain/                        # entidades JPA
│   ├── Wallet.kt                  # saldo em caixa (linha única, id fixo = 1)
│   ├── Position.kt                # posição aberta por symbol (quantidade + preço médio)
│   ├── Order.kt                   # ordem executada (histórico, tabela "orders")
│   └── OrderSide.kt               # enum BUY/SELL
├── repository/                    # Spring Data JPA, uma interface por entidade
├── dto/                           # request/response, isolam a entidade da API
│   ├── OrderDtos.kt
│   └── PortfolioDtos.kt
└── exception/
    ├── TradingExceptions.kt       # InsufficientFunds, InsufficientPosition, UnknownSymbol
    └── GlobalExceptionHandler.kt  # @RestControllerAdvice traduz para o HTTP status certo

src/main/resources/db/migration/   # migrações Flyway
```

Fluxo de uma ordem: o `Controller` valida o DTO (`@Valid`), o `TradingService.placeOrder`
busca o preço (`PriceService`), confere saldo e posição, atualiza `Wallet` e `Position`,
grava a `Order`. O `GlobalExceptionHandler` traduz exceções de negócio em HTTP 409/404.

`symbol` usa o id da CoinGecko direto (`bitcoin`, `ethereum`, ...), sem tabela de
mapeamento ticker → id.

## Licença

[MIT](LICENSE).
