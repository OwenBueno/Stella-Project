import { Prop, Schema, SchemaFactory } from '@nestjs/mongoose';
import { HydratedDocument } from 'mongoose';
import { stellaSchemaOptions } from '../schema-options';

export type DailyIntentDocument = HydratedDocument<DailyIntent>;

@Schema({ ...stellaSchemaOptions, collection: 'DailyIntent' })
export class DailyIntent {
  @Prop({ type: String, required: true })
  _id!: string;

  @Prop({ required: true, unique: true })
  date!: string;

  @Prop({ type: [String], required: true })
  top3TaskIds!: string[];

  @Prop({ type: Date, required: true })
  completedAt!: Date;

  @Prop({ required: true })
  nfcTagId!: string;

  @Prop({ type: Date, required: true })
  updatedAt!: Date;
}

export const DailyIntentSchema = SchemaFactory.createForClass(DailyIntent);
DailyIntentSchema.index({ updatedAt: 1 });
