import { Injectable, NotFoundException } from '@nestjs/common';
import { InjectModel } from '@nestjs/mongoose';
import { Model } from 'mongoose';
import { mapDoc } from '../database/document.util';
import { EveningReview, EveningReviewDocument } from '../database/schemas/evening-review.schema';
import { CreateEveningReviewDto } from './dto/create-evening-review.dto';

@Injectable()
export class EveningReviewsService {
  constructor(
    @InjectModel(EveningReview.name)
    private readonly eveningReviewModel: Model<EveningReviewDocument>,
  ) {}

  async getByDate(date: string) {
    const doc = await this.eveningReviewModel.findOne({ date }).exec();
    if (!doc) throw new NotFoundException(`Evening review for ${date} not found`);
    return mapDoc(doc)!;
  }

  async upsert(dto: CreateEveningReviewDto) {
    const doc = await this.eveningReviewModel
      .findOneAndUpdate(
        { date: dto.date },
        {
          _id: dto.id,
          date: dto.date,
          plannedVsActual: dto.plannedVsActual ?? null,
          reflectionText: dto.reflectionText ?? null,
          habitGridSnapshot: dto.habitGridSnapshot,
          completedAt: new Date(dto.completedAt),
          updatedAt: new Date(dto.updatedAt),
        },
        { upsert: true, new: true, setDefaultsOnInsert: true },
      )
      .exec();
    return mapDoc(doc)!;
  }
}
