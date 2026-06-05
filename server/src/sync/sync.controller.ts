import { Body, Controller, Get, Post, Query } from '@nestjs/common';
import { SyncPushDto } from './dto/sync-push.dto';
import { SyncService } from './sync.service';

@Controller('sync')
export class SyncController {
  constructor(private readonly syncService: SyncService) {}

  @Post('push')
  push(@Body() dto: SyncPushDto) {
    return this.syncService.push(dto);
  }

  @Get('pull')
  pull(
    @Query('since') since?: string,
    @Query('entity') entity?: string,
    @Query('limit') limit?: string,
    @Query('cursor') cursor?: string,
  ) {
    return this.syncService.pull(since, entity, limit, cursor);
  }
}
