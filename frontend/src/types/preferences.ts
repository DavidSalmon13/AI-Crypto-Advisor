export type InvestorType = 'HODLER' | 'DAY_TRADER' | 'NFT_COLLECTOR';
export type ContentType = 'MARKET_NEWS' | 'CHARTS' | 'SOCIAL' | 'FUN';

export interface CoinOption {
  id: string;
  symbol: string;
  name: string;
}

export interface PreferencesOptions {
  coins: CoinOption[];
  investorTypes: InvestorType[];
  contentTypes: ContentType[];
}

export interface Preferences {
  investorType: InvestorType;
  interests: string[];
  contentTypes: ContentType[];
}
