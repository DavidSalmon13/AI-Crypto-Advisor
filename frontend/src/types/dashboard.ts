/** Mirrors backend DashboardResponse (specs.md §4.3) field-for-field. */

/** 1 = 👍, -1 = 👎, null = not voted yet. */
export type Vote = 1 | -1 | null;

/** Voteable sections — Coin Prices is raw data, not curated content, so it is excluded. */
export type ItemType = 'NEWS' | 'AI_INSIGHT' | 'MEME';

export interface CoinPrice {
  id: string;
  symbol: string;
  name: string;
  priceUsd: number;
  change24hPct: number;
}

export interface NewsItem {
  id: string;
  title: string;
  url: string;
  source: string;
  publishedAt: string;
  userVote: Vote;
}

export interface AiInsight {
  id: string;
  text: string;
  generatedAt: string;
  fallback: boolean;
  userVote: Vote;
}

export interface Meme {
  id: string;
  imageUrl: string;
  caption: string;
  userVote: Vote;
}

export interface Dashboard {
  date: string;
  coinPrices: CoinPrice[];
  marketNews: NewsItem[];
  aiInsight: AiInsight;
  meme: Meme;
}
