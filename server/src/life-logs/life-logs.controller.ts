import { Controller, Get, Query } from '@nestjs/common';
import { LifeLogsService } from './life-logs.service';

@Controller('life-logs')
export class LifeLogsController {
  constructor(private readonly lifeLogsService: LifeLogsService) {}

  @Get()
  list(
    @Query('since') since?: string,
    @Query('limit') limit?: string,
    @Query('cursor') cursor?: string,
  ) {
    return this.lifeLogsService.list(since, limit, cursor);
  }
}
