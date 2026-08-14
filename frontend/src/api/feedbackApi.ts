import apiClient from '../lib/apiClient';
import type { ItemType } from '../types/dashboard';

export interface FeedbackResponse {
  id: string;
  itemType: ItemType;
  itemRef: string;
  vote: 1 | -1;
}

export async function castVote(
  itemType: ItemType,
  itemRef: string,
  vote: 1 | -1,
): Promise<FeedbackResponse> {
  const { data } = await apiClient.post<FeedbackResponse>('/api/feedback', { itemType, itemRef, vote });
  return data;
}

export async function retractVote(itemType: ItemType, itemRef: string): Promise<void> {
  await apiClient.delete(`/api/feedback/${itemType}/${encodeURIComponent(itemRef)}`);
}
