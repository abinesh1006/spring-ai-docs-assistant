# Getting started with the Northwind Payments API

Northwind Payments is a fictional payments API used as sample content for this assistant. Replace the files in
`docs/` with your own Markdown to make the assistant answer about your product.

## Environments

There are two environments. **Sandbox** (`https://sandbox.api.northwind.example`) accepts test card numbers and
never moves money. **Live** (`https://api.northwind.example`) processes real payments. Keys from one environment
do not work in the other.

## API keys

Every request needs an API key in the `Authorization` header: `Authorization: Bearer nw_live_...`. Keys starting
with `nw_test_` belong to the sandbox. Create and revoke keys under **Settings → Developers → API keys**. A revoked
key stops working within one minute. Never put a live key in a mobile app or in front-end code; call the API from
your server instead.

## Creating a payment

Send `POST /v1/payments` with an amount in the smallest currency unit and a currency code:

```json
{ "amount": 2500, "currency": "INR", "customer_id": "cus_123", "description": "Order 1042" }
```

The response contains the payment `id` and a `status` of `requires_confirmation`, `processing`, `succeeded` or
`failed`. A payment is final only when its status is `succeeded`; use webhooks to learn when that happens.

## Idempotency keys

Network errors can leave you unsure whether a request reached the API. Send an `Idempotency-Key` header with a
unique value (a UUID works) on every `POST`. If the same key is sent again within 24 hours, the API returns the
original response instead of creating a second payment. Reusing a key with a different request body returns
`409 idempotency_key_reused`.

## Test cards

In the sandbox, card `4242 4242 4242 4242` always succeeds, `4000 0000 0000 0002` is always declined, and
`4000 0000 0000 3220` requires 3-D Secure authentication.
