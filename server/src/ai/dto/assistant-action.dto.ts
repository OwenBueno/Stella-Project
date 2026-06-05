export interface AssistantActionDto {
  id: string;
  type: string;
  summary: string;
  payload: Record<string, unknown>;
}

export interface ParsedChatResult {
  reply: string;
  actions: AssistantActionDto[];
}
