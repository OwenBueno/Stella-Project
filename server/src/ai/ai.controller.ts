import { Body, Controller, Get, Param, Post, Query } from '@nestjs/common';
import { AiService } from './ai.service';
import { ChatRequestDto } from './dto/chat-request.dto';
import { CoachEvaluateRequestDto } from './dto/coach-evaluate.dto';
import { CoachSignalsRequestDto } from './dto/coach-signals.dto';

@Controller('ai')
export class AiController {
  constructor(private readonly aiService: AiService) {}

  @Post('chat')
  chat(@Body() dto: ChatRequestDto) {
    return this.aiService.chat(dto);
  }

  @Post('coach-evaluate')
  coachEvaluate(@Body() dto: CoachEvaluateRequestDto) {
    return this.aiService.coachEvaluate(dto);
  }

  @Post('coach-signals')
  coachSignals(@Body() dto: CoachSignalsRequestDto) {
    return this.aiService.coachSignals(dto);
  }

  @Get('threads/:threadId/messages')
  listMessages(
    @Param('threadId') threadId: string,
    @Query('limit') limit?: string,
    @Query('cursor') cursor?: string,
  ) {
    return this.aiService.listThreadMessages(threadId, limit, cursor);
  }
}
