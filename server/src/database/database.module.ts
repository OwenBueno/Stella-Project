import { Global, Module } from '@nestjs/common';
import { MongooseModule } from '@nestjs/mongoose';
import { IndexSyncService } from './index-sync.service';
import { stellaModels } from './schemas';

@Global()
@Module({
  imports: [
    MongooseModule.forRoot(process.env.DATABASE_URL ?? 'mongodb://127.0.0.1:27017/stella'),
    MongooseModule.forFeature(stellaModels),
  ],
  providers: [IndexSyncService],
  exports: [MongooseModule],
})
export class DatabaseModule {}
