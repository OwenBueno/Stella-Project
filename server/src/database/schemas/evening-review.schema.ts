import { Prop, Schema, SchemaFactory } from '@nestjs/mongoose';
import { HydratedDocument, Schema as MongooseSchema } from 'mongoose';
import { stellaSchemaOptions } from '../schema-options';

export type EveningReviewDocument = HydratedDocument<EveningReview>;

@Schema({ ...stellaSchemaOptions, collection: 'EveningReview' })
export class EveningReview {
  @Prop({ type: String, required: true })
  _id!: string;

  @Prop({ required: true, unique: true })
  date!: string;

  @Prop({ type: String, default: null })
  plannedVsActual!: string | null;

  @Prop({ type: String, default: null })
  reflectionText!: string | null;

  @Prop({ type: MongooseSchema.Types.Mixed, required: true })
  habitGridSnapshot!: Record<string, unknown>;

  @Prop({ type: Date, required: true })
  completedAt!: Date;

  @Prop({ type: Date, required: true })
  updatedAt!: Date;
}

export const EveningReviewSchema = SchemaFactory.createForClass(EveningReview);
EveningReviewSchema.index({ updatedAt: 1 });
