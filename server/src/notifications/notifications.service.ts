import {
  BadRequestException,
  Injectable,
  Logger,
  NotFoundException,
  OnModuleDestroy,
  OnModuleInit,
  ServiceUnavailableException,
} from '@nestjs/common';
import { InjectModel } from '@nestjs/mongoose';
import { Model } from 'mongoose';
import { DeviceToken, DeviceTokenDocument } from '../database/schemas/device-token.schema';
import { Task, TaskDocument } from '../database/schemas/task.schema';
import { FcmProvider } from './fcm.provider';
import { RegisterDeviceDto } from './dto/register-device.dto';
import { TestPushDto } from './dto/test-push.dto';

export interface CoachPushPayload {
  title: string;
  body: string;
  deepLink?: string;
}

@Injectable()
export class NotificationsService implements OnModuleInit, OnModuleDestroy {
  private readonly logger = new Logger(NotificationsService.name);
  private interval: ReturnType<typeof setInterval> | null = null;

  constructor(
    @InjectModel(Task.name) private readonly taskModel: Model<TaskDocument>,
    @InjectModel(DeviceToken.name)
    private readonly deviceTokenModel: Model<DeviceTokenDocument>,
    private readonly fcm: FcmProvider,
  ) {}

  onModuleInit() {
    this.interval = setInterval(() => {
      void this.logUpcomingDirectives();
    }, 15 * 60 * 1000);
    void this.logUpcomingDirectives();
  }

  onModuleDestroy() {
    if (this.interval) {
      clearInterval(this.interval);
      this.interval = null;
    }
  }

  health() {
    return {
      status: 'ok',
      mode: 'client_alarms',
      fcm: this.fcm.isEnabled() ? 'enabled' : 'disabled',
    };
  }

  async registerDevice(dto: RegisterDeviceDto) {
    const now = new Date();
    await this.deviceTokenModel
      .findOneAndUpdate(
        { deviceId: dto.deviceId },
        {
          _id: dto.deviceId,
          deviceId: dto.deviceId,
          fcmToken: dto.fcmToken,
          updatedAt: now,
        },
        { upsert: true, new: true },
      )
      .exec();
    return { registered: true, deviceId: dto.deviceId, updatedAt: now.toISOString() };
  }

  async sendTestPush(dto: TestPushDto) {
    const tokenDoc = await this.resolveTokenDoc(dto.deviceId);
    const messageId = await this.sendDataMessage(tokenDoc.fcmToken, {
      title: dto.title ?? 'Stella',
      body: dto.body ?? 'FCM test push — tap to open.',
      deepLink: dto.deepLink ?? 'home',
    });
    return { sent: true, messageId, deviceId: tokenDoc.deviceId };
  }

  async sendCoachPush(payload: CoachPushPayload, deviceId?: string) {
    const tokenDoc = await this.resolveTokenDoc(deviceId);
    const messageId = await this.sendDataMessage(tokenDoc.fcmToken, {
      title: payload.title,
      body: payload.body,
      deepLink: payload.deepLink ?? 'assistant',
    });
    return { sent: true, messageId };
  }

  private async resolveTokenDoc(deviceId?: string): Promise<DeviceTokenDocument> {
    const doc = deviceId
      ? await this.deviceTokenModel.findOne({ deviceId }).exec()
      : await this.deviceTokenModel.findOne().sort({ updatedAt: -1 }).exec();
    if (!doc?.fcmToken) {
      throw new NotFoundException({
        code: 'FCM_TOKEN_NOT_FOUND',
        message: deviceId
          ? `No FCM token for device ${deviceId}`
          : 'No FCM token registered — open app with google-services.json configured',
      });
    }
    return doc;
  }

  private async sendDataMessage(
    fcmToken: string,
    payload: { title: string; body: string; deepLink: string },
  ): Promise<string> {
    if (!this.fcm.isEnabled()) {
      throw new ServiceUnavailableException({
        code: 'FCM_NOT_CONFIGURED',
        message: 'Set FCM_SERVICE_ACCOUNT_JSON on the server',
      });
    }
    try {
      const result = await this.fcm.messaging().send({
        token: fcmToken,
        data: {
          type: 'stella_push',
          title: payload.title,
          body: payload.body,
          deepLink: payload.deepLink,
        },
        android: {
          priority: 'high',
        },
      });
      return result;
    } catch (err) {
      const msg = err instanceof Error ? err.message : String(err);
      this.logger.warn(`FCM send failed: ${msg}`);
      throw new BadRequestException({
        code: 'FCM_SEND_FAILED',
        message: msg,
      });
    }
  }

  private async logUpcomingDirectives() {
    const now = new Date();
    const windowEnd = new Date(now.getTime() + 15 * 60 * 1000);
    const due = await this.taskModel
      .find({
        deletedAt: null,
        status: { $ne: 'DONE' },
        scheduledAt: { $gte: now, $lte: windowEnd },
      })
      .select({ _id: 1, title: 1, scheduledAt: 1 })
      .limit(20)
      .exec();
    if (due.length === 0) return;
    this.logger.log(
      `Upcoming directives (next 15m, device fires locally): ${due.map((t) => `${t.title} @ ${t.scheduledAt?.toISOString()}`).join('; ')}`,
    );
  }
}
