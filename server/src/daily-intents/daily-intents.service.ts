import { Injectable, NotFoundException } from '@nestjs/common';
import { InjectModel } from '@nestjs/mongoose';
import { Model } from 'mongoose';
import { upsertByNaturalKey } from '../common/mongo/client-id-upsert';
import { mapDoc } from '../database/document.util';
import { DailyIntent, DailyIntentDocument } from '../database/schemas/daily-intent.schema';
import { CreateDailyIntentDto } from './dto/create-daily-intent.dto';

@Injectable()
export class DailyIntentsService {
  constructor(
    @InjectModel(DailyIntent.name)
    private readonly dailyIntentModel: Model<DailyIntentDocument>,
  ) {}

  async getByDate(date: string) {
    const doc = await this.dailyIntentModel.findOne({ date }).exec();
    if (!doc) throw new NotFoundException(`Daily intent for ${date} not found`);
    return mapDoc(doc)!;
  }

  async upsert(dto: CreateDailyIntentDto) {
    const doc = await upsertByNaturalKey(
      this.dailyIntentModel,
      { date: dto.date },
      {
        plannedTaskIds: dto.plannedTaskIds,
        completedAt: new Date(dto.completedAt),
        nfcTagId: dto.nfcTagId,
        updatedAt: new Date(dto.updatedAt),
      },
      {
        _id: dto.id,
        date: dto.date,
      },
    );
    return mapDoc(doc)!;
  }
}
