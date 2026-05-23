You are working in the TradingApp codebase.

Goal:
Move “Accumulation Strategy” out of the Transactions page into its own dedicated page with backend-driven pagination, asset filtering, createdAt DESC ordering, and backend-driven grouping totals by asset.

Current state:
- Frontend currently renders Accumulation Strategy inside:
  frontend/src/pages/TransactionsPage.tsx
  frontend/src/components/AccumulationStrategySection.tsx
- It uses accumulationTrades from Redux bootstrap:
  frontend/src/store/tradingSlice.ts
- API currently returns all accumulation trades as a plain array:
  GET /api/accumulation-trades
  implemented in:
  src/main/java/com/trading/controller/AccumulationTradeController.java
  src/main/java/com/trading/service/transaction/AccumulationTradeService.java
  src/main/java/com/trading/service/transaction/AccumulationTradeServiceImpl.java
  src/main/java/com/trading/domain/repository/AccumulationTradeRepository.java
- Existing response DTO:
  src/main/java/com/trading/dto/transaction/AccumulationTradeResponse.java

Required backend changes:
1. Change GET /api/accumulation-trades so it supports:
   - page: number, default 0
   - size: number, default 20
   - assetId: UUID optional
   - status: AccumulationTradeStatus optional
   - userId: UUID optional, preserving existing user validation
   - ordering MUST be createdAt DESC in backend
   - response MUST be a Spring Page / paginated response, same shape used by transactions.

2. Add repository methods using Pageable:
   - find by user id
   - find by user id + status
   - find by user id + asset id
   - find by user id + status + asset id
   All ordered by createdAt DESC via Pageable Sort.

3. Add backend grouping endpoint:
   GET /api/accumulation-trades/grouped-by-asset
   Query params:
   - status optional, default CLOSED if appropriate for accumulation delta totals
   - assetId optional if useful, but grouping should normally return all assets
   - userId optional with same validation
   Response should be list of rows:
   {
     assetId,
     totalAccumulationDelta,
     tradeCount
   }
   The sum must be calculated in the backend using SUM(accumulationDelta) or derived equivalent if accumulationDelta can be null. Only include trades where accumulation delta is meaningful, normally CLOSED trades.

4. Create a DTO, for example:
   AccumulationTradeAssetSummaryResponse(
     UUID assetId,
     BigDecimal totalAccumulationDelta,
     long tradeCount
   )

5. Make sure existing open/close accumulation trade endpoints still work.

Required frontend changes:
1. Remove AccumulationStrategySection from TransactionsPage.
   - Transactions pagination must remain only for Transactions data.
   - Do not display accumulation strategy on /transactions anymore.

2. Create a new page:
   frontend/src/pages/AccumulationStrategyPage.tsx

3. Add route:
   /accumulation-strategy
   in frontend/src/app/router.tsx under RequireAuth.

4. Add navigation item in:
   frontend/src/components/AppHeader.tsx
   Suggested label: “Accumulation Strategy”
   Put it near Transactions / Strategies.

5. Update frontend API:
   frontend/src/api/tradingApi.ts
   - listAccumulationTrades should return PaginatedResponse<AccumulationTradeItem>
   - params should include page, size, status, assetId, userId
   - add listAccumulationTradeAssetSummaries or similar for grouped-by-asset endpoint.

6. Update types:
   frontend/src/types/trading.ts
   Add:
   AccumulationTradeAssetSummaryItem {
     assetId: string;
     totalAccumulationDelta: string;
     tradeCount: number;
   }

7. Redux:
   Either add separate state to tradingSlice or manage locally in the new page, but the new page must support:
   - accumulationTrades page content
   - current page
   - page size
   - total pages
   - total elements
   - selected asset filter
   - grouped summary rows
   Loading must call backend with page, size, assetId, status, userId.

8. New page UI requirements:
   - Title: “Accumulation Strategy”
   - Asset filter dropdown using existing assets list.
   - Pagination controls similar to Transactions page.
   - Main table ordered by backend createdAt DESC.
   - Columns:
     Asset
     Exit Price
     Reentry Price
     Old Coin Amount
     New Coin Amount
     Accumulation Delta
     Status
     Created At
     Closed At
   - Add a button/toggle/section for “Group by asset” or “Asset totals”.
   - Grouped totals must come from backend endpoint, not frontend calculation.
   - Grouped table/card should show:
     Asset
     Total Accumulation Delta
     Trade Count

9. Backend filtering, pagination, grouping, and ordering must NOT be done only in frontend.

10. Keep formatting consistent:
   - Use existing formatNumber, formatUsd, formatDateTime.
   - Use existing AssetOption lookup to display asset symbol instead of UUID.

11. Clean up:
   - If AccumulationStrategySection is no longer needed, delete it.
   - Remove unused imports and state from TransactionsPage.
   - Do not break buy/sell/open/close accumulation flow from TransactionHistoryTable.
   - Keep bootstrap loading assets/exchanges/transactions working.

Acceptance criteria:
- /transactions shows only transaction data and its own pagination.
- /accumulation-strategy shows accumulation trades with independent backend pagination.
- Asset filter changes reload data from backend.
- Results are ordered by createdAt DESC from backend.
- Grouped totals per asset are loaded from backend, not computed in React.
- Existing accumulation open/close functionality still works.
- App builds without TypeScript or Java compilation errors.