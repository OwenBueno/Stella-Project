import { Injectable, Logger, OnModuleInit } from '@nestjs/common';
import { readFileSync } from 'fs';
import * as admin from 'firebase-admin';

@Injectable()
export class FcmProvider implements OnModuleInit {
  private readonly logger = new Logger(FcmProvider.name);
  private ready = false;

  onModuleInit() {
    this.ready = this.tryInitialize();
  }

  isEnabled(): boolean {
    return this.ready;
  }

  messaging(): admin.messaging.Messaging {
    if (!this.ready) {
      throw new Error('FCM_NOT_CONFIGURED');
    }
    return admin.messaging();
  }

  private tryInitialize(): boolean {
    const raw = process.env.FCM_SERVICE_ACCOUNT_JSON?.trim();
    if (!raw) {
      this.logger.warn('FCM_SERVICE_ACCOUNT_JSON not set — push disabled');
      return false;
    }
    try {
      const credential = this.parseCredential(raw);
      if (!admin.apps.length) {
        admin.initializeApp({
          credential: admin.credential.cert(credential),
        });
      }
      this.logger.log('Firebase Admin initialized for FCM');
      return true;
    } catch (err) {
      this.logger.error(`FCM init failed: ${err instanceof Error ? err.message : err}`);
      return false;
    }
  }

  private parseCredential(raw: string): admin.ServiceAccount {
    const json =
      raw.startsWith('{') ? raw : readFileSync(raw, 'utf8');
    return JSON.parse(json) as admin.ServiceAccount;
  }
}
