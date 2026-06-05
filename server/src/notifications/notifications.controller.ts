import { Body, Controller, Get, Post } from '@nestjs/common';
import { Public } from '../common/decorators/public.decorator';
import { RegisterDeviceDto } from './dto/register-device.dto';
import { CoachNudgeDto } from './dto/coach-nudge.dto';
import { TestPushDto } from './dto/test-push.dto';
import { NotificationsService } from './notifications.service';

@Controller('notifications')
export class NotificationsController {
  constructor(private readonly notificationsService: NotificationsService) {}

  @Public()
  @Get('health')
  health() {
    return this.notificationsService.health();
  }

  @Post('register')
  register(@Body() dto: RegisterDeviceDto) {
    return this.notificationsService.registerDevice(dto);
  }

  @Post('test')
  testPush(@Body() dto: TestPushDto) {
    return this.notificationsService.sendTestPush(dto);
  }

  @Post('coach-nudge')
  coachNudge(@Body() dto: CoachNudgeDto) {
    return this.notificationsService.sendCoachPush(
      { title: dto.title, body: dto.body, deepLink: dto.deepLink },
      dto.deviceId,
    );
  }
}
